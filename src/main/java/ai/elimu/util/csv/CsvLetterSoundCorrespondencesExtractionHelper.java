package ai.elimu.util.csv;

import ai.elimu.dao.LetterDao;
import ai.elimu.dao.LetterSoundCorrespondenceDao;
import ai.elimu.dao.SoundDao;
import ai.elimu.model.content.Letter;
import ai.elimu.model.content.LetterSoundCorrespondence;
import ai.elimu.model.content.Sound;
import ai.elimu.web.content.letter_sound_correspondence.LetterSoundCorrespondenceCsvExportController;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CsvLetterSoundCorrespondencesExtractionHelper {

    private static final Logger logger = LogManager.getLogger();

    /**
     * For information on how the CSV files were generated, see {@link LetterSoundCorrespondenceCsvExportController#handleRequest}.
     */
    public static List<LetterSoundCorrespondence> getLetterSoundCorrespondencesFromCsvBackup(File csvFile, LetterDao letterDao, SoundDao soundDao, LetterSoundCorrespondenceDao letterSoundCorrespondenceDao) {
        logger.info("getLetterSoundCorrespondencesFromCsvBackup");

        List<LetterSoundCorrespondence> letterSoundCorrespondences = new ArrayList<>();

        Path csvFilePath = Paths.get(csvFile.toURI());
        logger.info("csvFilePath: " + csvFilePath);
        try {
            Reader reader = Files.newBufferedReader(csvFilePath);
            CSVFormat csvFormat = CSVFormat.DEFAULT
                .withHeader(
                    "id",
                    "letter_ids",
                    "letter_texts",
                    "sound_ids",
                    "sound_values_ipa",
                    "usage_count"
                )
                .withSkipHeaderRecord();
            CSVParser csvParser = new CSVParser(reader, csvFormat);
            for (CSVRecord csvRecord : csvParser) {
                logger.info("csvRecord: " + csvRecord);

                LetterSoundCorrespondence letterSoundCorrespondence = new LetterSoundCorrespondence();

                JSONArray letterIdsJsonArray = new JSONArray(csvRecord.get("letter_ids"));
                logger.info("letterIdsJsonArray: " + letterIdsJsonArray);

                JSONArray letterTextsJsonArray = new JSONArray(csvRecord.get("letter_texts"));
                logger.info("letterTextsJsonArray: " + letterTextsJsonArray);
                List<Letter> letters = new ArrayList<>();
                for (int i = 0; i < letterTextsJsonArray.length(); i++) {
                    String letterText = letterTextsJsonArray.getString(i);
                    logger.info("Looking up Letter with text '" + letterText + "'");
                    Letter letter = letterDao.readByText(letterText);
                    logger.info("letter.getId(): " + letter.getId());
                    letters.add(letter);
                }
                letterSoundCorrespondence.setLetters(letters);

                JSONArray soundIdsJsonArray = new JSONArray(csvRecord.get("sound_ids"));
                logger.info("soundIdsJsonArray: " + soundIdsJsonArray);

                JSONArray soundValuesIpaJsonArray = new JSONArray(csvRecord.get("sound_values_ipa"));
                logger.info("soundValuesIpaJsonArray: " + soundValuesIpaJsonArray);
                List<Sound> sounds = new ArrayList<>();
                for (int i = 0; i < soundValuesIpaJsonArray.length(); i++) {
                    String soundValueIpa = soundValuesIpaJsonArray.getString(i);
                    logger.info("Looking up Sound with IPA value /" + soundValueIpa + "/");
                    Sound sound = soundDao.readByValueIpa(soundValueIpa);
                    logger.info("sound.getId(): " + sound.getId());
                    sounds.add(sound);
                }
                letterSoundCorrespondence.setSounds(sounds);

                Integer usageCount = Integer.valueOf(csvRecord.get("usage_count"));
                letterSoundCorrespondence.setUsageCount(usageCount);

                letterSoundCorrespondences.add(letterSoundCorrespondence);
            }
        } catch (IOException ex) {
            logger.error(ex);
        }

        return letterSoundCorrespondences;
    }
}
