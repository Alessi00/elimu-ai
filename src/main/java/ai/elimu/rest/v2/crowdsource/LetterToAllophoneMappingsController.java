package ai.elimu.rest.v2.crowdsource;

import ai.elimu.dao.LetterToAllophoneMappingDao;
import ai.elimu.model.content.LetterToAllophoneMapping;
import ai.elimu.model.v2.gson.content.LetterToAllophoneMappingGson;
import ai.elimu.rest.v2.JpaToGsonConverter;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping(value = "/rest/v2/crowdsource/word-contributions", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class LetterToAllophoneMappingsController {

    private final Logger logger = LogManager.getLogger();

    @Autowired
    private LetterToAllophoneMappingDao letterToAllophoneMappingDao;

    /**
     * This method will return the letter to allophone mappings
     * TODO : fetch other information that are optional when creating a new word eg: existing words(for Root word) etc.
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/letter-to-allophone-mappings", method = RequestMethod.GET)
    public String getWordDataForCrowdSourcing(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        logger.info("handleGetRequest");

        JSONArray letterToAllophoneMappingsJsonArray = new JSONArray();
        for (LetterToAllophoneMapping letterToAllophoneMapping : letterToAllophoneMappingDao.readAllOrderedByUsage()) {
            LetterToAllophoneMappingGson letterToAllophoneMappingGson = JpaToGsonConverter.getLetterToAllophoneMappingGson(letterToAllophoneMapping);
            String json = new Gson().toJson(letterToAllophoneMappingGson);
            letterToAllophoneMappingsJsonArray.put(new JSONObject(json));
        }

        String jsonResponse = letterToAllophoneMappingsJsonArray.toString();
        logger.info("jsonResponse: " + jsonResponse);
        return jsonResponse;
    }

}
