package com.example.ltapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    /**
     * Declare all the UI Elements from the XML File.
     */
    private Spinner toSpinner, fromSpinner;
    private TextInputEditText sourceText;
    private ImageView micImageView, textToSpeechImageView;
    private MaterialButton translateButton;
    private TextView translatedText;

    private TextToSpeech textToSpeech;

    /**
     * Declare all the languages available for translation (can be extended).
     */
    String[] fromLanguages = { "From",  "English", "Afrikaans", "Arabic", "Belarusian", "Bulgarian", "Bengali",
                                "Catalan", "Czech", "Welsh", "Hindi", "Urdu", "French", "German", "Spanish"};
    String[] toLanguages = { "To",  "English", "Afrikaans", "Arabic", "Belarusian", "Bulgarian", "Bengali",
            "Catalan", "Czech", "Welsh", "Hindi", "Urdu", "French", "German", "Spanish"};


    /**
     * Request codes for handling intents.
     */
    private static final int MIC_REQUEST_CODE = 1;
    private static final int CAMERA_REQUEST_CODE = 1888;

    /**
     * Language codes for translation
     */
    String fromLanguageCode, toLanguageCode = "";


    /**
     * Set the buttons for reporting and camera on the menu bar.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Define behavior when a menu button is pressed.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        /**
         * When report button is pressed, open a dialog box to get the details of the report to send.
         */
        if(item.getItemId() == R.id.menu_report) {

            new ReportDialogFragment().show(this.getSupportFragmentManager(), "REPORT");
            return true;

//            Toast.makeText(this, "Report a translation with error", Toast.LENGTH_SHORT).show();
        }
        /**
         * When camera button is pressed, open the camera to take the picture.
         */
        if(item.getItemId() == R.id.menu_camera) {
//            Toast.makeText(this, "Image Text Recognition", Toast.LENGTH_SHORT).show();
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /**
         * Initialize the UI Elements
         */
        toSpinner = findViewById(R.id.toSpinner);
        fromSpinner = findViewById(R.id.fromSpinner);
        sourceText = findViewById(R.id.textSource);
        micImageView = findViewById(R.id.micIV);
        textToSpeechImageView = findViewById(R.id.textToSpeechIV);
        translateButton = findViewById(R.id.translateBtn);
        translatedText = findViewById(R.id.translatedTextTV);


        /**
         * When a particular language is selected from the spinner, set the "from language" to the selected language.
         */
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fromLanguageCode = getLanguageCode(fromLanguages[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter fromAdapter = new ArrayAdapter(this, R.layout.spinner_item, fromLanguages);
        fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(fromAdapter);

        /**
         * When a particular language is selected from the spinner, set the "from language" to the selected language.
         */
        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toLanguageCode = getLanguageCode(toLanguages[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter toAdapter = new ArrayAdapter(this, R.layout.spinner_item, toLanguages);
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toSpinner.setAdapter(toAdapter);

        translateButton.setOnClickListener(v -> {
            translatedText.setText("");
            //Check if source text is empty.
            if(sourceText.getText().toString().isEmpty()) {
                Toast.makeText(MainActivity.this, "Please Enter Some Text", Toast.LENGTH_SHORT).show();
            }
            //Check if from language is empty.
            else if(fromLanguageCode.isEmpty()) {
                Toast.makeText(this, "Please Select a Source Language", Toast.LENGTH_SHORT).show();
            }
            //Check if to language is empty.
            else if(toLanguageCode.isEmpty()) {
                Toast.makeText(this, "Please Select a Target Language", Toast.LENGTH_SHORT).show();
            }
            //Translate the text if all parameters are correctly entered.
            else {
                translateText(fromLanguageCode, toLanguageCode, sourceText.getText().toString());
            }
        });

        /**
         * Handle speech-to-text
         */
        micImageView.setOnClickListener(v -> {
            //Create an intent to record audio and recognize words from it.
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something to convert to text");
            try {
                startActivityForResult(intent, MIC_REQUEST_CODE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        textToSpeechImageView.setOnClickListener(v -> {
            textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != TextToSpeech.ERROR)
                        textToSpeech.setLanguage(Locale.forLanguageTag(toLanguageCode));
                    textToSpeech.speak(translatedText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                }
            });

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Handle the audio intent
        if(requestCode == MIC_REQUEST_CODE) {
            if(resultCode == RESULT_OK && data != null) {
                //Get the text received by converting from audio and set the source text.
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                sourceText.setText(result.get(0));
            }
        }
        //Handle the camera intent
        else if(requestCode == CAMERA_REQUEST_CODE) {
            if(resultCode == RESULT_OK && data != null) {
                //Get the Bitmap photo from the camera
                Bitmap photo = (Bitmap) data.getExtras().get("data");
//                testImageView.setImageBitmap(photo);

                //Initialize the Text Recognizer.
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                //Set the input image to the image received from camera
                InputImage image = InputImage.fromBitmap(photo, 0);

                //Extract the text from the above image
                Task<Text> res = recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text text) {
                                //Set the source text to the text extracted from the photo
                                String resultText = text.getText();
                                sourceText.setText(resultText);
//                                for(Text.TextBlock block: text.getTextBlocks()) {
//                                    String blockText = block.getText();
//                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "Error getting text from image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        }
    }


    /**
     * Handle Text Translation
     * @param fromLanguageCode
     * @param toLanguageCode
     * @param source
     */
    private void translateText(String fromLanguageCode, String  toLanguageCode, String source) {
        translatedText.setText("Downloading model. This may take a while...");

        //Initialize the translator
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(fromLanguageCode)
                .setTargetLanguage(toLanguageCode)
                .build();
        Translator translator = Translation.getClient(options);

        //If the model is not available offline, try to download the model.
        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        translatedText.setText("Translating...");
                        //Translate the source text with the help of the downloaded model.
                        translator.translate(source)
                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                    @Override
                                    public void onSuccess(String s) {
                                        //Set the target text to the translated text.
                                        translatedText.setText(s);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Failed to translate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to download model: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /**
     * Handle getting the language code from the language selected from spinner.
     * @param fromLanguage
     * @return
     */
    private String getLanguageCode(String fromLanguage) {
        String languageCode = "";

        switch (fromLanguage){
            case "English":
                languageCode = TranslateLanguage.ENGLISH;
                break;
            case "Afrikaans":
                languageCode = TranslateLanguage.AFRIKAANS;
                break;
            case "Arabic":
                languageCode = TranslateLanguage.ARABIC;
                break;
            case "Belarusian":
                languageCode = TranslateLanguage.BELARUSIAN;
                break;
            case "Bulgarian":
                languageCode = TranslateLanguage.BULGARIAN;
                break;
            case "Bengali":
                languageCode = TranslateLanguage.BENGALI;
                break;
            case "Catalan":
                languageCode = TranslateLanguage.CATALAN;
                break;
            case "Czech":
                languageCode = TranslateLanguage.CZECH;
                break;
            case "Welsh":
                languageCode = TranslateLanguage.WELSH;
                break;
            case "Hindi":
                languageCode = TranslateLanguage.HINDI;
                break;
            case "Urdu":
                languageCode = TranslateLanguage.URDU;
                break;
            case "French":
                languageCode = TranslateLanguage.FRENCH;
                break;
            case "German":
                languageCode =  TranslateLanguage.GERMAN;
                break;
            case "Spanish":
                languageCode = TranslateLanguage.SPANISH;
                break;
            default:
                languageCode = "";

        }

        return languageCode;
    }
}