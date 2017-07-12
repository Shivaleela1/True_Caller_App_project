package com.example.abhi.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    ServiceHandler serviceHandler;
    EditText number;
    Button search;
    TextView tv;
    ArrayList<Contact> contacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isNetworkAvailable(MainActivity.this)) {
            //available network
            fetchContacts();

            serviceHandler = new ServiceHandler();

            //take control of UI components
            number = (EditText) findViewById(R.id.editText);
            search = (Button) findViewById(R.id.button);
            tv = (TextView) findViewById(R.id.tv);

            search.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {

                    //search for specific phone no in server
                    new GetContactFromServer().execute(number.getText().toString());
                }


            });
        }else {      //network not available
            getActionBar().setTitle("No Internet");
        }
    }




    private void fetchContacts() {

        contacts=new ArrayList<Contact>();
        String phoneNumber=null;

        //constants for the contacts table having columns id, name, phone nos associated
        Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;//path to contacts app database
        String _ID = ContactsContract.Contacts._ID;//primary key
        String DISPLAY_NAME = ContactsContract.Contacts.DISPLAY_NAME;
        String HAS_PHONE_NUMBER = ContactsContract.Contacts.HAS_PHONE_NUMBER;

        //constants for the phone table having columns contact id, phone no
        Uri Phone_CONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
        String NUMBER=ContactsContract.CommonDataKinds.Phone.NUMBER;

        //initialize content resolver object to work with content provider
        ContentResolver contentResolver = getContentResolver();//refer you to the database fields

        //read contacts table data
        Cursor cursor = contentResolver.query(CONTENT_URI,null,null,null,null);

        //loop for every contact in the phone
        if(cursor.getCount()>0){

            //main data table
            while (cursor.moveToNext()){ //-1 --> 0

                String contact_id = cursor.getString(cursor.getColumnIndex(_ID));

                String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));

                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(HAS_PHONE_NUMBER)));

                if(hasPhoneNumber>0 && !name.equals("")){

                    //Query and loop for every Phone no of the contact
                    Cursor phoneCursor = contentResolver.query( //passing same query as before
                            Phone_CONTENT_URI,  //table reference
                            null,
                            Phone_CONTACT_ID + "=  ? ",
                            new String[] {contact_id},null);//specific query to retrieve values which I require
                       //this is called primary key and foreign key linkage

                    //table1 --> contact_id
                    //table2_phone --> PHONE_CONTACT_ID = contact_id

                    //sub table to retrieve phone no
                    assert phoneCursor != null;
                    while (phoneCursor.moveToNext()){
                        phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(NUMBER));

                        //replaces { } - , with ""
                        phoneNumber = phoneNumber.replace("(","").replace(")","").replace("-","").trim().replace(" ","");

                        Contact contact = new Contact();
                        contact.setPhoneNumber(phoneNumber);
                        contact.setName(name);

                        contacts.add(contact);
                    }

                    phoneCursor.close();
                }

                //issues send contact request to the server
                try {
                    new SendContactsToServer().execute(contacts);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    //isNetworkAvailable method checks the network connectivity and returns true if network is available
    //or else false
    //check network availability
    public boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo()!=null;
    }


    //background Async task to send contact to server using HTTP request
    private class SendContactsToServer extends AsyncTask <ArrayList<Contact>,String,String> {

        //post data to server
        @Override
        protected String doInBackground(ArrayList<Contact>... args) {

            for(int i=0;i<args[0].size();i++){

                Contact contact = args[0].get(i);//getting data and retrieving it
                //building parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();

                //C:path
                params.add(new BasicNameValuePair(Constant.KEY_NAME,contact.getName()));
                params.add(new BasicNameValuePair(Constant.KEY_PHONE_NUMBER,contact.getPhoneNumber()));

                //posting JSON string to server URL
                try {
                    serviceHandler.makeServiceCall(Constant.POST_CONTACTS,2,params);
                }catch (NullPointerException e){

                }

            }
            return "";
        }



            @Override
            protected void onPostExecute(String response) {
                super.onPostExecute(response);
                JSONObject jsonObjectContact;
                try {
                    jsonObjectContact = new JSONObject(response);
                    int status = Integer.parseInt(jsonObjectContact.getString(Constant.KEY_SUCCESS));
                    if(status == 1){
                        JSONArray data = jsonObjectContact.getJSONArray(Constant.KEY_CONTACT);
                        JSONObject singleContact = data.getJSONObject(0);
                        String name = singleContact.getString(Constant.KEY_NAME);
                        tv.setText("This is : "+name);
                    }else {
                        tv.setText("Unknown Number");
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }

    }
    //background Async Task to get contact from server by making HTTP request
    class GetContactFromServer extends AsyncTask<String,String,String>{
        protected String doInBackground(String... args) {
            //building parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair(Constant.KEY_PHONE_NUMBER, args[0]));
            //get JSON string response from server URL
            String response = serviceHandler.makeServiceCall(Constant.GET_CONTACTS, 1, params);
            return response;
        }
    }

}
