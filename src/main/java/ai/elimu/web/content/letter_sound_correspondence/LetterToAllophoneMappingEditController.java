package ai.elimu.web.content.letter_sound_correspondence;

import java.util.List;
import javax.validation.Valid;

import org.apache.logging.log4j.Logger;
import ai.elimu.dao.AllophoneDao;
import ai.elimu.dao.LetterDao;
import ai.elimu.dao.LetterToAllophoneMappingDao;
import ai.elimu.model.content.Allophone;
import ai.elimu.model.content.Letter;
import ai.elimu.model.content.LetterToAllophoneMapping;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/content/letter-sound-correspondence/edit")
public class LetterToAllophoneMappingEditController {
    
    private final Logger logger = LogManager.getLogger();
    
    @Autowired
    private LetterToAllophoneMappingDao letterSoundCorrespondenceDao;
    
    @Autowired
    private LetterDao letterDao;
    
    @Autowired
    private AllophoneDao allophoneDao;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String handleRequest(Model model, @PathVariable Long id) {
    	logger.info("handleRequest");
        
        LetterToAllophoneMapping letterSoundCorrespondence = letterSoundCorrespondenceDao.read(id);
        model.addAttribute("letterSoundCorrespondence", letterSoundCorrespondence);
        
        List<Letter> letters = letterDao.readAllOrdered();
        model.addAttribute("letters", letters);
        
        List<Allophone> allophones = allophoneDao.readAllOrdered();
        model.addAttribute("allophones", allophones);

        return "content/letter-sound-correspondence/edit";
    }
    
    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public String handleSubmit(
            @Valid LetterToAllophoneMapping letterSoundCorrespondence,
            BindingResult result,
            Model model
    ) {
    	logger.info("handleSubmit");
        
        // Check if the LetterToAllophoneMapping already exists
        LetterToAllophoneMapping existingLetterToAllophoneMapping = letterSoundCorrespondenceDao.read(letterSoundCorrespondence.getLetters(), letterSoundCorrespondence.getAllophones());
        if ((existingLetterToAllophoneMapping != null) && !existingLetterToAllophoneMapping.getId().equals(letterSoundCorrespondence.getId())) {
            result.rejectValue("letters", "NonUnique");
        }
        
        if (result.hasErrors()) {
            model.addAttribute("letterSoundCorrespondence", letterSoundCorrespondence);
            
            List<Letter> letters = letterDao.readAllOrdered();
            model.addAttribute("letters", letters);
            
            List<Allophone> allophones = allophoneDao.readAllOrdered();
            model.addAttribute("allophones", allophones);
            
            return "content/letter-sound-correspondence/edit";
        } else {
            letterSoundCorrespondenceDao.update(letterSoundCorrespondence);
            
            return "redirect:/content/letter-sound-correspondence/list#" + letterSoundCorrespondence.getId();
        }
    }
}