package ai.elimu.web;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import ai.elimu.dao.ContributorDao;
import ai.elimu.model.contributor.Contributor;
import ai.elimu.model.enums.Environment;
import ai.elimu.model.enums.Role;
import ai.elimu.util.ConfigHelper;
import ai.elimu.web.context.EnvironmentContextLoaderListener;
import org.apache.logging.log4j.LogManager;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Google2Api;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * See https://console.developers.google.com/apis
 */
@Controller
public class SignOnControllerGoogle {
	
    private OAuthService oAuthService;

    private Token requestToken;

    private Logger logger = LogManager.getLogger();
    
    @Autowired
    private ContributorDao contributorDao;

    @RequestMapping("/sign-on/google")
    public String handleAuthorization(HttpServletRequest request) throws IOException {
        logger.info("handleAuthorization");
		
        String apiKey = "108974530651-3g9r40r5s7it6p9vjh2e2eplgmm1to0d.apps.googleusercontent.com";
        String apiSecret = "mGlUmzxL2eP69HdVmPKtVLR7";
        String baseUrl = "http://localhost:8080/webapp";
        if (EnvironmentContextLoaderListener.env == Environment.TEST) {
            apiKey = "108974530651-fskde869tac7imherk2k516shfuvij76.apps.googleusercontent.com";
            apiSecret = ConfigHelper.getProperty("google.api.secret");
            baseUrl = "http://" + request.getServerName();
        } else if (EnvironmentContextLoaderListener.env == Environment.PROD) {
            apiKey = "108974530651-k68pccps2jb88fllofpcf8ht356v08e4.apps.googleusercontent.com";
            apiSecret = ConfigHelper.getProperty("google.api.secret");
            baseUrl = "http://" + request.getServerName();
        }

        oAuthService = new ServiceBuilder()
                .provider(Google2Api.class) // See https://gist.github.com/2465453
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .callback(baseUrl + "/sign-on/google/callback")
                .scope("email https://www.googleapis.com/auth/plus.login") // https://developers.google.com/+/web/api/rest/oauth#login-scopes
                .build();

        logger.info("Fetching the Authorization URL...");
        String authorizationUrl = oAuthService.getAuthorizationUrl(requestToken);
        logger.info("Got the Authorization URL!");

        return "redirect:" + authorizationUrl;
    }

    @RequestMapping(value="/sign-on/google/callback", method=RequestMethod.GET)
    public String handleCallback(HttpServletRequest request, Model model) {
        logger.info("handleCallback");

        if (request.getParameter("error") != null) {
            return "redirect:/sign-on?error=" + request.getParameter("error");
        } else {
            String verifierParam = request.getParameter("code");
            logger.debug("verifierParam: " + verifierParam);
            Verifier verifier = new Verifier(verifierParam);
            String responseBody = null;
            try {
                Token accessToken = oAuthService.getAccessToken(requestToken, verifier);
                logger.debug("accessToken: " + accessToken);

                OAuthRequest oAuthRequest = new OAuthRequest(Verb.GET, "https://www.googleapis.com/oauth2/v1/userinfo");
                oAuthService.signRequest(accessToken, oAuthRequest);
                Response response = oAuthRequest.send();
                responseBody = response.getBody();
                logger.info("response.getCode(): " + response.getCode());
                logger.info("response.getBody(): " + responseBody);
            } catch (OAuthException e) {
                logger.error(e);
                return "redirect:/sign-on?error=" + e.getMessage();
            }

            Contributor contributor = new Contributor();
            try {
                JSONObject jsonObject = new JSONObject(responseBody);
                logger.info("jsonObject: " + jsonObject);

                if (jsonObject.has("email")) {
                    // TODO: validate e-mail
                    contributor.setEmail(jsonObject.getString("email"));
                }
                if (jsonObject.has("id")) {
                    contributor.setProviderIdGoogle(jsonObject.getString("id"));
                }
                if (jsonObject.has("picture")) {
                    contributor.setImageUrl(jsonObject.getString("picture"));
                }
                if (jsonObject.has("given_name")) {
                    contributor.setFirstName(jsonObject.getString("given_name"));
                }
                if (jsonObject.has("family_name")) {
                    contributor.setLastName(jsonObject.getString("family_name"));
                }
            } catch (JSONException e) {
                logger.error(e);
            }

            Contributor existingContributor = contributorDao.read(contributor.getEmail());
            if (existingContributor == null) {
                // Store new Contributor in database
                contributor.setRegistrationTime(Calendar.getInstance());
                if (contributor.getEmail().endsWith("@elimu.ai")) {
                    contributor.setRoles(new HashSet<>(Arrays.asList(Role.ADMIN, Role.ANALYST, Role.CONTRIBUTOR)));
                } else {
                    contributor.setRoles(new HashSet<>(Arrays.asList(Role.CONTRIBUTOR)));
                }
                contributorDao.create(contributor);
            } else {
                // Contributor already exists in database
                
                // Update existing contributor with latest values fetched from provider
                if (StringUtils.isNotBlank(contributor.getProviderIdGoogle())) {
                    existingContributor.setProviderIdGoogle(contributor.getProviderIdGoogle());
                }
                if (StringUtils.isNotBlank(contributor.getImageUrl())) {
                    existingContributor.setImageUrl(contributor.getImageUrl());
                }
                // TODO: firstName/lastName
                contributorDao.update(existingContributor);
                
                // Contributor registered previously
                contributor = existingContributor;
            }

            // Authenticate
            new CustomAuthenticationManager().authenticateUser(contributor);

            // Add Contributor object to session
            request.getSession().setAttribute("contributor", contributor);

            return "redirect:/content";
        }
    }
}
