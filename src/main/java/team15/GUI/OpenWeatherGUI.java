package team15.GUI;

/**
 * This class is the main GUI for the OpenWeather API program. It handles the
 * loading and saving of data during the session. 
 * 
 * As well it handles building the entire GUI, changing the users preferences,
 * and refreshing all the weather and forecast data.
 * 
 * Views supported are a local view with a customizable display, a short term
 * forecast over 24h in 3h increments, and a long term forecast that is daily
 * over 8 days.
 * 
 * @author team15
 */

//Imports
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import org.json.JSONException;
import team15.UserOjects.User;

public class OpenWeatherGUI extends JFrame{
    //User variable
    private static User user;
    
    //Container for tabs
    private final JTabbedPane tabbedPane;
    
    //Tabs
    private final LocalPanel local;
    private final ForecastPanel shortTerm, longTerm;

    /**
     * The main GUI window class for the OpenWeather API program.
     */
    public OpenWeatherGUI(){
        super();

        //Frame settings
        this.setTitle("Team 15 Weather");
        this.setSize(1200, 800); 
        this.setLocation(100,50);
        this.getContentPane().setLayout(new BoxLayout
                                          (getContentPane(), BoxLayout.X_AXIS));
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Create the tab pages
        this.local = new LocalPanel();
        this.shortTerm = new ForecastPanel();
        this.longTerm = new ForecastPanel();
        
        //Populate the panels with weather and forecast data
        this.updatePanels();
        
        //Create new tabbed pane
        this.tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBackground(Color.LIGHT_GRAY);
        tabbedPane.setPreferredSize(new Dimension(5, 800));
        tabbedPane.setMinimumSize(new Dimension(5, 500));
        tabbedPane.setSize(new Dimension(0, 500));

        //Add the tabs
        tabbedPane.addTab("Current", local);
        tabbedPane.addTab("Short Term Forecast", shortTerm);
        tabbedPane.addTab("Longterm Forecast", longTerm);
        this.getContentPane().add(tabbedPane);

        //Create the menu bar
        JMenuBar menuBar = new JMenuBar();
        
        //Add the menu bar
        this.setJMenuBar(menuBar);
        
        //Create a new JMenu
        JMenu menu = new JMenu("Menu");
        menu.getAccessibleContext()
                            .setAccessibleDescription("Select difference page");
        menuBar.add(menu);
        
        //Create the menu items
        JMenuItem ref = new JMenuItem("Refresh");
        JMenuItem preferences = new JMenuItem ("Preferences");
        JMenuItem locationList = new JMenuItem ("Locations Menu");
        
        //Add the menu items
        menu.add(preferences);
        menu.add(locationList);
        menu.add(ref);
        
        //Add the menu item action listeners
        //Refresh action listener
        ref.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event){
                refresh();
            }
        });
        
        //Preferences action listener
        preferences.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event){
                //Open the edit preferences dialog
                PreferencesDialog preferences = new PreferencesDialog(user);
                
                //Check if the preferences were saved if no display an error
                if(preferences.wasUpdated()) updatePanels();
                else{
                    updateError("Error: failed to save user "
                                                  + "data to the local drive.");
                }
            }
        });
        
        //Locations action listener
        locationList.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event){
                startLoactionDialog();
            }
        });
           
        //Set the main GUI visible
        this.setVisible(true);
        
        //If the user has no locations make sure that they add at least one
        if(user.getLocations().isEmpty()){
            startLoactionDialog();
            
            if(user.getLocations().isEmpty()){
                System.out.println("Location list can not be empty.");
                System.exit(1);
            }
        }
        
        //Try and refresh the data
        this.refresh();       
    }
    
    /**
     * Tries to refresh the weather and forecast data. 
     * 
     * If this is not possible due to an internet error, an object construction 
     * error, or because the previous data refresh or poll was updated recently
     * then it updates the panels with appropriate error messages.
     */
    private void refresh(){
        String error = "";
        
        /* Try to update the weather and forecast data for the user's current
         * location */
        try{
            error = user.getCurrentLocation().updateForecasts();
        //An internet error has occured
        } catch (IOException ex) {
            this.updateError("Error: problem connecting to OpenWeather.com");
            return;
        //The json returned was not created properly
        } catch (JSONException ex) {
            this.updateError("Error: Unable to get data from OpenWeather.com");
            return;
        }
        
        /* It the weather was not polled because it has been too long since
         * the last refresh or OpenWeather Api poll */
        if(!error.isEmpty()){
            this.updateError(error);
            return;
        }
        
        //Try to save the new user object with the updated forecasts
        try{
            user.saveUser();
        }
        catch(Exception e){
            error = "Error: failed to save user data to the local drive.";
        }
        
        //Update the panels with the new weather data and the error message
        this.updateError(error);
        this.updatePanels();
    }
    
    /**
     * Updates each panel with the most recent weather data, forecast data, 
     * location data and preferences.
     */
    private void updatePanels(){
        String location = user.getCurrentLocation().toString();
        String refresh = user.getCurrentLocation().getRefresh();
        boolean units = user.pref.tempUnits;
        
        local.update(user.getLocalWeather(), user.pref, location, refresh);
        shortTerm.update(user.getShortTermForecast(), units, location, refresh);
        longTerm.update(user.getLongTermForecast(), units, location, refresh);
    }
    
    /**
     * Updates all the forecast tabs with the given error string
     * @param error the error the is to be displayed on each tab
     */
    private void updateError(String error){
        local.setError(error);
        shortTerm.setError(error);
        longTerm.setError(error);
    }
    
    /**
     * Creates the location dialog and handles any possible errors it may
     * produce.
     */
    private void startLoactionDialog(){
        LocationsDialog window = null;
        
        try{
            window = new LocationsDialog(user);
        } catch (IOException ex) {
            updateError("Failed to load OpenWeather possible "
                                       + "location list from citylist.txt");
        }
        
        if(window != null) window.dispose();
        
        //Update the panels
        this.updatePanels();
    }
    
    /**
     * The main function of the OpenWeather Api program.
     * 
     * It deals with the loading or creating of the user object and then
     * calls the constructor of the GUI
     * @param args unused
     */
    public static void main(String args[]){
        user = null;
        
        //Try to load the previously saved user file from user.dat
        try {
            user = User.loadUser();
        } catch (IOException ex) {
            File f = new File("user.dat");
            if(f.isFile()){
                System.out.println("Error: user.dat exists but could "
                                                            + "not be loaded.");
                return;
            }
        } catch (ClassNotFoundException ex) {
            System.out.println("Error: User class not found when loading "
                                       + "user.dat.  Likely version mismatch.");
            return;
        }
        
        //If the user was not loaded from user.dat create a new user object
        if(user == null){
            user = new User();
            
            try {
                user.saveUser();
            } catch (IOException ex) {
                System.out.println("Error: Could not save the initial User "
                                                        + "object to user.dat");
                return;
            }
        }
        
        new OpenWeatherGUI();
    }
}