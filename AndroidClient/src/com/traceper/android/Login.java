package com.traceper.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.traceper.R;
import com.traceper.android.interfaces.IAppService;
import com.traceper.android.services.AppService;

public class Login extends Activity {	

    protected static final int NOT_CONNECTED_TO_SERVICE = 0;
	protected static final int FILL_BOTH_USERNAME_AND_PASSWORD = 1;
	private static final int MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT = 2 ;
	private static final int NOT_CONNECTED_TO_NETWORK = 3;
	private static final int UNKNOWN_ERROR_OCCURED = 4;
	private static final int SETTINGS_DIALOG = 5;
	private static final int HTTP_REQUEST_FAILED = 6;
	private static final int HTTP_MISSING_PARAMETER = 7;
	private static final int CUSTOM_MESSAGE_DIALOG = 8;
	private EditText emailText;
    private EditText passwordText;
    private Button cancelButton;
    private CheckBox rememberMeCheckBox;
    private String dialogMessage;
    private IAppService appManager;
    private ProgressDialog progressDialog;
    public static final int SIGN_UP_ID = Menu.FIRST;
    public static final int SETTINGS_ID = Menu.FIRST + 1;
    public static final int EXIT_APP_ID = Menu.FIRST + 2;
	
   

   
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            appManager = ((AppService.IMBinder)service).getService();  
            appManager.setAuthenticationServerAddress(getSharedPreferences(Configuration.PREFERENCES_NAME, 0).getString(Configuration.PREFERENCES_SERVER_INDEX, Configuration.DEFAULT_SERVER_ADRESS));
           
            if (appManager.isNetworkConnected() == false)
    		{
    			showDialog(NOT_CONNECTED_TO_NETWORK);					
    		}
            else if (appManager.isUserAuthenticated() == true)
            {
            	Intent i = new Intent(Login.this, Main.class);																
				startActivity(i);
				Login.this.finish();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
        	appManager = null;
            Toast.makeText(Login.this, R.string.local_service_stopped,
                    Toast.LENGTH_SHORT).show();
        }
    };
	
	
	
    
    
    /** Called when the activity is first created. */	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    

        setContentView(R.layout.login_screen);
        setTitle("Login - " + Configuration.APPLICATION_NAME);
        
        Button loginButton = (Button) findViewById(R.id.login);
        cancelButton = (Button) findViewById(R.id.cancel_login);
        emailText = (EditText) findViewById(R.id.email);
        passwordText = (EditText) findViewById(R.id.password);   
        rememberMeCheckBox = (CheckBox) findViewById(R.id.remember_me_checkbox);
    
        
        SharedPreferences preferences = getSharedPreferences(Configuration.PREFERENCES_NAME, 0);
        emailText.setText(preferences.getString(Configuration.PREFERENCES_USEREMAIL, ""));
        passwordText.setText(preferences.getString(Configuration.PREFERENCES_PASSWORD, ""));
        rememberMeCheckBox.setChecked(preferences.getBoolean(Configuration.PREFRENCES_REMEMBER_ME_CHECKBOX, false));
        
        
        loginButton.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) 
			{	
				SharedPreferences.Editor editor = getSharedPreferences(Configuration.PREFERENCES_NAME, 0).edit();
				editor.putBoolean(Configuration.PREFRENCES_REMEMBER_ME_CHECKBOX, rememberMeCheckBox.isChecked());
				editor.commit();
				// Start and bind the  imService 
		    	startService(new Intent(Login.this,  AppService.class));
		    	
				if (appManager == null) {
					showDialog(NOT_CONNECTED_TO_SERVICE);
					return;
				}
				else if (appManager.isNetworkConnected() == false)
				{
					showDialog(NOT_CONNECTED_TO_NETWORK);					
				}
				//TODO: check whether email format is valid.
				else if (emailText.length() > 0 && passwordText.length() > 0)
				{					
					progressDialog = ProgressDialog.show(Login.this, "", getString(R.string.loading), true, false);	
					
					Thread loginThread = new Thread(){
						private Handler handler = new Handler();
						String result;
						@Override
						public void run() {
							result = appManager.authenticateUser(emailText.getText().toString(), passwordText.getText().toString());
							
							handler.post(new Runnable(){
								public void run() {										
									progressDialog.dismiss();
									
									if (result.equals("1")) // == IAppService.HTTP_RESPONSE_SUCCESS)
									{
										SharedPreferences.Editor editor = getSharedPreferences(Configuration.PREFERENCES_NAME, 0).edit();
										
										if (rememberMeCheckBox.isChecked() == true) {									
				                        	editor.putString(Configuration.PREFERENCES_USEREMAIL, emailText.getText().toString());
				                        	editor.putString(Configuration.PREFERENCES_PASSWORD, passwordText.getText().toString());		                        									
										}
										else {
											editor.remove(Configuration.PREFERENCES_USEREMAIL);
											editor.remove(Configuration.PREFERENCES_PASSWORD);
										}
										editor.commit();	
																		
										Intent i = new Intent(Login.this, Main.class);												
										//i.putExtra(FRIEND_LIST, result);						
										startActivity(i);	
										Login.this.finish();										
									}
									else{
										Login.this.dialogMessage = result;
										showDialog(CUSTOM_MESSAGE_DIALOG);
									}
								}									
							});
														
						}
					};
					
					loginThread.start();
					
				}
				else {
					// Username or Password is not filled, alert the user					 
					showDialog(FILL_BOTH_USERNAME_AND_PASSWORD);
				}
			}
			
        });
        
        cancelButton.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) 
			{					
				appManager.exit();
				finish();				
			}        	
        }); 
    }
    
    @Override
    protected Dialog onCreateDialog(int id) 
    {    	
    
    	switch (id) 
    	{
    		case NOT_CONNECTED_TO_SERVICE:
    		
    			return new AlertDialog.Builder(Login.this)       
        		.setMessage(R.string.not_connected_to_service)
        		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				/* User clicked OK so do some stuff */
        			}
        		})        
        		.create();    			
    			
    		case FILL_BOTH_USERNAME_AND_PASSWORD:
    		
    			return new AlertDialog.Builder(Login.this)       
        		.setMessage(R.string.fill_both_username_and_password)
        		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				/* User clicked OK so do some stuff */
        			}
        		})        
        		.create();    
    			
    		case MAKE_SURE_USERNAME_AND_PASSWORD_CORRECT:

    			return new AlertDialog.Builder(Login.this)       
        		.setMessage(R.string.make_sure_username_and_password_correct)
        		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				/* User clicked OK so do some stuff */
        			}
        		})        
        		.create();  
    			
    		case NOT_CONNECTED_TO_NETWORK:

    			return new AlertDialog.Builder(Login.this)       
        		.setMessage(R.string.not_connected_to_network)
        		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				finish();
        			}
        		})        
        		.create(); 
    			
    		case UNKNOWN_ERROR_OCCURED:

    			return new AlertDialog.Builder(Login.this)       
        		.setMessage(R.string.unknown_error_occured)
        		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				/* User clicked OK so do some stuff */
        			}
        		})        
        		.create(); 
    		
    		case SETTINGS_DIALOG:
    			
    			LayoutInflater factory = LayoutInflater.from(this);
                View textEntryView = factory.inflate(R.layout.server_address_entry_dialog, null);
                
                final EditText server_address_edit = (EditText) textEntryView.findViewById(R.id.server_address_edit);
              
                server_address_edit.setText(getSharedPreferences(Configuration.PREFERENCES_NAME, 0).
                									getString(Configuration.PREFERENCES_SERVER_INDEX, Configuration.DEFAULT_SERVER_ADRESS));
                
                return new AlertDialog.Builder(Login.this)
                   // .setIcon(R.drawable.alert_dialog_icon)
                    .setTitle(R.string.alert_dialog_settings)
                    .setView(textEntryView)
                    .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	SharedPreferences.Editor editor = getSharedPreferences(Configuration.PREFERENCES_NAME, 0).edit();
                        	editor.putString(Configuration.PREFERENCES_SERVER_INDEX, server_address_edit.getText().toString());
                        	editor.commit();
                        	appManager.setAuthenticationServerAddress(server_address_edit.getText().toString());
                        }
                    })
                    .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        	server_address_edit.setText(getSharedPreferences(Configuration.PREFERENCES_NAME, 0).
									getString(Configuration.PREFERENCES_SERVER_INDEX, Configuration.DEFAULT_SERVER_ADRESS));
                            /* User clicked cancel so do some stuff */
                        }
                    })
                    .create(); 
    		case HTTP_REQUEST_FAILED:
    			return new AlertDialog.Builder(Login.this)       
        		.setMessage(R.string.http_request_failed)
        		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				/* User clicked OK so do some stuff */
        			}
        		})        
        		.create(); 
    		case HTTP_MISSING_PARAMETER:
    			return new AlertDialog.Builder(Login.this)       
        		.setMessage(R.string.http_missing_parameter)
        		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				/* User clicked OK so do some stuff */
        			}
        		})        
        		.create(); 
    		case CUSTOM_MESSAGE_DIALOG:
    			return new AlertDialog.Builder(Login.this)       
        		.setMessage(this.dialogMessage)
        		.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
        			public void onClick(DialogInterface dialog, int whichButton) {
        				/* User clicked OK so do some stuff */
        			}
        		})        
        		.create(); 
    		
    		default:
    			return null;
    	}
    }

	@Override
	protected void onPause() 
	{
		unbindService(mConnection);
		super.onPause();
	}

	@Override
	protected void onResume() 
	{		
		bindService(new Intent(Login.this, AppService.class), mConnection , Context.BIND_AUTO_CREATE);
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		boolean result = super.onCreateOptionsMenu(menu);
		/* 
		 * show sign up menu item if registration is made enabled.
		 */
		if (Configuration.REGISTRATION_ENABLED == true) {
			 menu.add(0, SIGN_UP_ID, 0, R.string.register).setIcon(R.drawable.register);
		 }
		 menu.add(0, SETTINGS_ID, 0, R.string.settings).setIcon(R.drawable.settings);
		 
		 menu.add(0, EXIT_APP_ID, 0, R.string.exit_application).setIcon(R.drawable.exit);
		 
		return result;
	}
	

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
	    
		switch(item.getItemId()) 
	    {
	    	case SIGN_UP_ID:
	    		Intent i = new Intent(Login.this, Register.class);
	    		startActivity(i);
	    		return true;
	    	case SETTINGS_ID:
	    		showDialog(SETTINGS_DIALOG);
	    		break;
	    	case EXIT_APP_ID:
	    		cancelButton.performClick();
	    		return true;
	    }
	       
	    return super.onMenuItemSelected(featureId, item);
	}    
}