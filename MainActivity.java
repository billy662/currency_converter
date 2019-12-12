package com.exercise.currencyconverter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.R.anim;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Math.round;

public class MainActivity extends AppCompatActivity {
    TextView tvRate, tvCity, tvCountry, tvTemp, tvFeelsLike, tvPrecip, tvDesc, tvCurrencyHeader;
    EditText etSource, etTarget, etCustomCur, etCity, etTax;
    Button btnUpdateLive, btnSet;
    CheckBox cbTax;
    RadioButton rbTdy, rbTmr, rb2DaysLater, rb3DaysLater;
    Spinner baseCur, targetCur;
    fetchData task = null;
    fetchWeather weatherTask = null;
    double rate = 0;
    boolean editingSource = false, editingTarget = false, isFirstTimeFetchingData = true;
    String defaultCity = "Hong Kong";
    static int daysAfterToday = 0;
    final String[] currencies = {
        "AUD", "BGN", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "GBP", "HKD", "HRK", "HUF",
        "IDR", "ILS", "INR", "JPY", "KRW", "MXN", "MYR" ,"NOK" ,"NZD" , "PHP" ,"PLN" ,"RON" ,
        "RUB" ,"SEK" ,"SGD" ,"THB" ,"TRY" ,"USD", "ZAR"
    };
    String[] convert = {"AUD", "BGN"};
    SharedPreferences saveWeather, saveTax;
    JSONObject weatherJobj = new JSONObject();


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.help:
                //start HELP activity
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        tvRate = (TextView)findViewById(R.id.tvRate);
        tvCurrencyHeader = (TextView)findViewById(R.id.tvCurrencyHeader);
        tvCity = (TextView)findViewById(R.id.tvCity);
        tvCountry = (TextView)findViewById(R.id.tvCounty);
        tvTemp = (TextView)findViewById(R.id.tvTemp);
        tvFeelsLike = (TextView)findViewById(R.id.tvFeelsLike);
        tvPrecip = (TextView)findViewById(R.id.tvPrecip);
        tvDesc = (TextView)findViewById(R.id.tvDesc);
        etSource = (EditText)findViewById(R.id.etSource);
        etTarget = (EditText)findViewById(R.id.etTarget);
        cbTax = (CheckBox)findViewById(R.id.cbTax);
        etTax = (EditText)findViewById(R.id.etTax);
        etCustomCur = (EditText)findViewById(R.id.etCustomCur);
        etCity = (EditText)findViewById(R.id.etCity);
        rbTdy = (RadioButton)findViewById(R.id.rbTdy);
        rbTmr = (RadioButton)findViewById(R.id.rbTmr);
        rb2DaysLater = (RadioButton)findViewById(R.id.rb2DaysLater);
        rb3DaysLater = (RadioButton)findViewById(R.id.rb3DaysLater);
        btnUpdateLive = (Button)findViewById(R.id.btnUpdateLive);
        btnSet = (Button)findViewById(R.id.btnSet);
        baseCur = (Spinner)findViewById(R.id.baseCur);
        targetCur = (Spinner)findViewById(R.id.targetCur);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_layout, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        baseCur.setAdapter(adapter);
        targetCur.setAdapter(adapter);

        final SharedPreferences selectedCurrencies = getSharedPreferences("currency", 0);

        //set onCheckChangedListener for tax rate check box
        cbTax.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                etTax.setEnabled(cbTax.isChecked());
                Toast.makeText(getApplicationContext(), "Tax " + (cbTax.isChecked()?"included":"excluded"), Toast.LENGTH_SHORT).show();
                if(etSource.getText().toString().length() !=0 && etTarget.getText().toString().length() != 0){
                    editingSource = true;
                    etTarget.setText(convertCurrency("target"));
                    editingSource = false;
                }
            }
        });

        //set cbTax unchecked and EditText disabled as default
        cbTax.setChecked(false);
        etTax.setEnabled(false);

        //set tax rate min max value
        etTax.setFilters(new InputFilter[]{ new InputFilterMinMax(0, 100) });

        //set saved tax rate
        saveTax = getSharedPreferences("tax",0);
        String savedTax = saveTax.getString("taxRate", "");
        etTax.setText(savedTax);

        //set spinner default value
        int savedBase = selectedCurrencies.getInt("base",-1);
        if(savedBase > -1) {
            baseCur.setSelection(savedBase);
            convert[0] = currencies[savedBase];
        }
        else
            baseCur.setSelection(0);

        int savedTarget = selectedCurrencies.getInt("target", -1);
        if(savedTarget > -1) {
            targetCur.setSelection(savedTarget);
            convert[1] = currencies[savedTarget];
        }
        else
            targetCur.setSelection(1);

        //set weather field
        saveWeather = getSharedPreferences("city",0);
        String savedCity = saveWeather.getString("savedCity", "");
        defaultCity = (savedCity.equals(""))? "Hong Kong": savedCity;

        //set dropdown list listener
        baseCur.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int basePos = baseCur.getSelectedItemPosition();
                int targetPos = targetCur.getSelectedItemPosition();
                if(basePos == targetPos){
                    Toast.makeText(getApplicationContext(), "Please select a different currency", Toast.LENGTH_SHORT).show();
                    baseCur.setSelection(basePos+((basePos!=0)?-1:+1));
                    return;
                }
                convert[0] = currencies[baseCur.getSelectedItemPosition()];
                SharedPreferences.Editor editor = selectedCurrencies.edit();
                editor.putInt("base", position);
                editor.apply();
                updateCurrency();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){}
        });

        targetCur.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int basePos = baseCur.getSelectedItemPosition();
                int targetPos = targetCur.getSelectedItemPosition();
                if(basePos == targetPos){
                    Toast.makeText(getApplicationContext(), "Please select a different currency", Toast.LENGTH_SHORT).show();
                    targetCur.setSelection(targetPos+((targetPos!=0)?-1:+1));
                    return;
                }
                convert[1] = currencies[targetCur.getSelectedItemPosition()];
                SharedPreferences.Editor editor = selectedCurrencies.edit();
                editor.putInt("target", position);
                editor.apply();
                updateCurrency();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent){

            }
        });

        etSource.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editingSource = true;
                if(!editingTarget){
                    if(s.length() != 0){
                        if(s.toString().startsWith(".")){
                            etSource.setText("0.");
                            etSource.setSelection(etSource.getText().length());
                            return;
                        }
                        if(rate != 0) {
                            etTarget.setText(convertCurrency("target"));
                        }
                        else
                            etTarget.setText("0");
                    }
                    else{
                        etTarget.setText("");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                editingSource = false;
            }
        });

        etTarget.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                editingTarget = true;
                if(!editingSource){
                    if(s.length() != 0){
                        if(s.toString().startsWith(".")) {
                            etTarget.setText("0.");
                            etTarget.setSelection(etTarget.getText().length());
                            return;
                        }
                        if(rate != 0) {
                            etSource.setText(convertCurrency("source"));
                        }
                        else
                            etSource.setText("0");
                    }
                    else{
                        etSource.setText("");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                editingTarget = false;
            }
        });

        etTax.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences settings = getSharedPreferences("tax", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("taxRate", etTax.getText().toString());
                editor.apply();
                if (!etTax.getText().toString().equals("") && etSource.getText().toString().length() != 0 && etTarget.getText().toString().length() != 0){
                    editingSource = true;
                    etTarget.setText(convertCurrency("target"));
                    editingSource = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etCustomCur.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().startsWith(".")) {
                    etCustomCur.setText("0.");
                    etCustomCur.setSelection(etCustomCur.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                editingTarget = false;
            }
        });

        rbTdy.setChecked(true);

        //set the date of the day after tomorrow to the radio button
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 2);
        date = c.getTime();
        String twoDaysAfter = dateFormat.format(date);
        rb2DaysLater.setText(twoDaysAfter);
        c.add(Calendar.DATE, 1);
        date = c.getTime();
        String threeDaysAfter = dateFormat.format(date);
        rb3DaysLater.setText(threeDaysAfter);

        //set uncheck behaviours manually as the radio buttons are put in a layout
        //and set onClickListener for radio buttons to change weather information
        rbTdy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbTmr.setChecked(false);
                rb2DaysLater.setChecked(false);
                rb3DaysLater.setChecked(false);
                searchWeather();
                MainActivity.daysAfterToday = 0;
            }
        });

        rbTmr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbTdy.setChecked(false);
                rb2DaysLater.setChecked(false);
                rb3DaysLater.setChecked(false);
                searchWeather();
                MainActivity.daysAfterToday = 1;
            }
        });

        rb2DaysLater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbTdy.setChecked(false);
                rbTmr.setChecked(false);
                rb3DaysLater.setChecked(false);
                searchWeather();
                MainActivity.daysAfterToday = 2;
            }
        });

        rb3DaysLater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rbTdy.setChecked(false);
                rbTmr.setChecked(false);
                rb2DaysLater.setChecked(false);
                searchWeather();
                MainActivity.daysAfterToday = 3;
            }
        });

        SharedPreferences custRate = getSharedPreferences("rate",0);
        double savedRate = Double.parseDouble(custRate.getString("customRate","0"));
        if(savedRate != 0){
            rate = savedRate;
            tvRate.setText(String.valueOf(savedRate));
            etCustomCur.setText(String.valueOf(savedRate));
        }
        else{
            updateCurrency();
        }
        etCity.setText(defaultCity);
        getWeather(defaultCity);
    }

    private class fetchData extends AsyncTask<String, Integer, String>{
        ProgressDialog dialog;

        @Override
        protected String doInBackground(String... values){
            InputStream inputStream = null;
            String JSONResult = "";
            URL url = null;

            try{
                //url = new URL("http://api.fixer.io/latest?base=JPY&symbols=HKD");
                url = new URL("http://api.fixer.io/latest?base="+ convert[0] +"&symbols="+ convert[1]);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                inputStream = con.getInputStream();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(inputStream)
                );
                String line = "";
                while((line = br.readLine()) != null)
                    JSONResult += line;
                inputStream.close();

                JSONObject jobj = new JSONObject(JSONResult);
                rate = jobj.getJSONObject("rates").getDouble(convert[1]);
            } catch (Exception e){
                return e.toString();
            }
            return "ok";
        }

        @Override
        protected void onPreExecute(){
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Fetching data from server...");
            dialog.show();
            btnUpdateLive.setText("Updating...");
            btnUpdateLive.setEnabled(false);
        }

        @Override
        protected void onPostExecute(String result){
            if(rate != 0){
                //asyncTask return "ok"
                tvRate.setText(String.valueOf(rate));
                if(etSource.getText().toString().length() !=0 && etTarget.getText().toString().length() != 0) {
                    editingSource = true;
                    etTarget.setText(convertCurrency("target"));
                    editingSource = false;
                }
            }
            else{
                Toast.makeText(getApplicationContext(),"Failed to get currecny from server\nPlease try again", Toast.LENGTH_LONG).show();
            }
            if(dialog.isShowing())
                dialog.dismiss();
            btnUpdateLive.setText("Update to latest currency");
            btnUpdateLive.setEnabled(true);
            tvCurrencyHeader.setText("Current Currency("+ convert[0] +" to "+ convert[1] +")");
        }
    }

    public void onClickUpdateLive(View v){
        updateCurrency();
    }

    public void onClickUpdateCust(View v){
        if(etCustomCur.getText().length() > 0){
            SharedPreferences settings = getSharedPreferences("rate",0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("customRate",etCustomCur.getText().toString());
            editor.apply();
            Toast.makeText(this, "Currency set", Toast.LENGTH_SHORT).show();
            tvRate.setText(settings.getString("customRate","0"));
            rate = Double.parseDouble(settings.getString("customRate","0"));
            if(etSource.getText().toString().length() !=0 && etTarget.getText().toString().length() != 0){
                editingSource = true;
                etTarget.setText(convertCurrency("target"));
                editingSource = false;
            }
        }
        else
            Toast.makeText(this, "Please enter the currency", Toast.LENGTH_SHORT).show();
    }

    public void onClickSearchWeather(View v){
        searchWeather();
    }

    public void searchWeather(){
        String inCity = etCity.getText().toString();
        if(inCity.equals("")){
            Toast.makeText(getApplicationContext(), "Please enter city", Toast.LENGTH_LONG).show();
            return;
        }
        getWeather(inCity);
        SharedPreferences.Editor editor = saveWeather.edit();
        editor.putString("savedCity", inCity);
        editor.apply();
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void getWeather(String city){
        //fetch weather
        try{
            if (weatherTask == null || weatherTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
                weatherTask = new fetchWeather();
                weatherTask.execute(city);
            }
        } catch (Exception e){
            Toast.makeText(getApplicationContext(), "Failed to search country: " + city, Toast.LENGTH_SHORT).show();
        }
    }

    private class fetchWeather extends AsyncTask<String, Integer, String[]>{
        //weather values
        String city, country, desc, feelsLike;
        double temp, precip;

        @Override
        protected String[] doInBackground(String... values){
            InputStream inputStream = null;
            String JSONResult = "";
            URL url = null;
            String[] result = {"ok", values[0]};

            try{
                url = new URL("http://api.apixu.com/v1/forecast.json?key=4f2ba1b68fb149d3b9521129171508&q="  + values[0] + "&days=4");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                inputStream = con.getInputStream();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(inputStream)
                );
                String line = "";
                while((line = br.readLine()) != null)
                    JSONResult += line;
                inputStream.close();

                weatherJobj = new JSONObject(JSONResult);

                city = weatherJobj.getJSONObject("location").getString("name") + ", " +
                        weatherJobj.getJSONObject("location").getString("region");
                country = weatherJobj.getJSONObject("location").getString("country");
                if(daysAfterToday == 0){
                    temp = weatherJobj.getJSONObject("current").getDouble("temp_c");
                    feelsLike = weatherJobj.getJSONObject("current").getString("feelslike_c");
                    precip = weatherJobj.getJSONObject("current").getDouble("precip_mm");
                    desc = weatherJobj.getJSONObject("current").getJSONObject("condition").getString("text");
                }
                else{
                    temp = weatherJobj.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(daysAfterToday).getJSONObject("day").getDouble("avgtemp_c");
                    feelsLike = "N/A";
                    precip = weatherJobj.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(daysAfterToday).getJSONObject("day").getDouble("totalprecip_mm");
                    desc = weatherJobj.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(daysAfterToday).getJSONObject("day").getJSONObject("condition").getString("text");
                }
            } catch (Exception e){
                result[0] = e.toString();
            }
            return result;
        }

        @Override
        protected void onPreExecute(){
            final String loadingText = "Loading...";

            tvCity.setText(loadingText);
            tvCountry.setText(loadingText);
            tvTemp.setText(loadingText);
            tvFeelsLike.setText(loadingText);
            tvPrecip.setText(loadingText);
            tvDesc.setText(loadingText);
        }

        @Override
        protected void onPostExecute(String[] result){
            if(result[0].equals("ok")){
                tvCity.setText(city);
                tvCountry.setText(country);
                tvTemp.setText(String.valueOf(temp));
                tvFeelsLike.setText(String.valueOf(feelsLike));
                tvPrecip.setText(String.valueOf(precip));
                tvDesc.setText(desc);
            }
            else{
                Toast.makeText(getApplicationContext(),"Failed to search country: " + result[1], Toast.LENGTH_LONG).show();
                final String failed = "Failed to load";

                tvCity.setText(failed);
                tvCountry.setText(failed);
                tvTemp.setText(failed);
                tvFeelsLike.setText(failed);
                tvPrecip.setText(failed);
                tvDesc.setText(failed);
            }
        }
    }

    private void updateCurrency(){
        try{
            if (task == null || task.getStatus().equals(AsyncTask.Status.FINISHED)) {
                task = new fetchData();
                task.execute("");
            }
        } catch (Exception e){
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public static double round(double value, int places){
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public String convertCurrency(String target){
        double tax = (!etTax.getText().toString().matches("") && cbTax.isChecked())?(1 + Double.parseDouble(etTax.getText().toString()) / 100):1;
        double output;
        if(target.equals("source")){
            tax = (!etTax.getText().toString().matches("") && cbTax.isChecked())?(1 + Double.parseDouble(etTax.getText().toString()) / 100):1;
            output = round((Double.parseDouble(etTarget.getText().toString()) * tax / rate), 2);
        }
        else
            output = round((Double.parseDouble(etSource.getText().toString()) * tax * rate), 2);
        return String.valueOf(output);
    }
}
