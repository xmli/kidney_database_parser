import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class LogParser {
	
	public static final String UPDATE_STR = "[type] => update";
	public static final String UPDATE_ADLIST_STR = "[type] => update_adlist";
	public static final String UPDATE_ANTIBODIES_STR = "[type] => update_antibodies";
	public static final String UPDATE_USER_STR = "[type] => update_user";
	
	public static final String STATUS_STR = "[status] => ";
	public static final String TRANSPLANT_STR = "[status] => Transplanted";
	public static final String INACTIVE_STR = "[status] => Inactive";
	public static final String ACTIVE_STR = "[status] => Active";
	public static final String REMOVED_STR = "[status] => Removed";
	
	public static final String PATIENTID_STR = "[patient_id] => ";
	public static final String PERSONID_STR = "[personId] => ";
	public static final String PERSONTYPE_STR = "[personType] => ";
	
	public static final String LASTTOUCHED_STR = "[last_touch] => ";
	
	private String patientID;
	private String personType;
	private String status;
	private String lastTouch;
	
	public LogParser(String patientID, String personType, String status, String lastTouch) {
		this.personType = personType;
		this.patientID = patientID;
		this.status = status;
		this.lastTouch = lastTouch;
	}
	
	public static String getPersonType(String dbquery) {
		int ptIndex = dbquery.indexOf(PERSONTYPE_STR);
		int nextSpace = dbquery.indexOf(" ", ptIndex);
		for(int i = 0; i < 2; i++) {
			ptIndex = nextSpace;
			nextSpace = dbquery.indexOf(" ", nextSpace+1);
		}
		String type = (dbquery.substring(ptIndex, nextSpace)).trim();
		return type;
	}
	
	public static String getPatientID(String dbquery) {
		String id = null;
		if(dbquery.contains(PERSONID_STR)) {
			int idIndex = dbquery.indexOf(PERSONID_STR);
			int nextSpace = dbquery.indexOf(" ", idIndex);
			for(int i = 0; i < 2; i++) {
				idIndex = nextSpace;
				nextSpace = dbquery.indexOf(" ", nextSpace+1);
			}
			id = (dbquery.substring(idIndex, nextSpace)).trim();
		} else if(dbquery.contains(PATIENTID_STR)) {
			int idIndex = dbquery.indexOf(PATIENTID_STR);
			int nextSpace = dbquery.indexOf(" ", idIndex);
			for(int i = 0; i < 2; i++) {
				idIndex = nextSpace;
				nextSpace = dbquery.indexOf(" ", nextSpace+1);
			}
			id = (dbquery.substring(idIndex, nextSpace)).trim();
		}
		return id;
	}
	
	public static String getStatus(String dbquery) {
		int statusIndex = dbquery.indexOf(STATUS_STR);
		int nextSpace = dbquery.indexOf(" ", statusIndex);
		for(int i = 0; i < 2; i++) {
			statusIndex = nextSpace;
			nextSpace = dbquery.indexOf(" ", nextSpace+1);
		}
		String status = (dbquery.substring(statusIndex, nextSpace)).trim();
		return status;
	}
	
	public static String getLastTouch(String dbquery) {
		int unixIndex = dbquery.indexOf(LASTTOUCHED_STR);
		int nextSpace = dbquery.indexOf(" ", unixIndex);
		for(int i = 0; i < 2; i++) {
			unixIndex = nextSpace;
			nextSpace = dbquery.indexOf(" ", nextSpace+1);
		}
		String unixTime = (dbquery.substring(unixIndex, nextSpace)).trim();
		return unixTime;
	}
	
	public static String epochToDate(String epoch) {
		if(!epoch.isEmpty()) {
			SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
			Date date = new Date(Long.parseLong(epoch) * 1000);
			return formatter.format(date);
		}
		return epoch;
	}
	
	public static String getStatusDate(ArrayList<LogParser> lpList) {
		LogParser firstLog = lpList.get(0);
		String flTranspDate = "";
		String flInactiveDate = "";
		String flRemovedDate = "";
		
		if(firstLog.status.equals("Transplanted")) {
			flTranspDate = firstLog.lastTouch;
		} else if(firstLog.status.equals("Inactive")) {
			flInactiveDate = firstLog.lastTouch;
		} else if(firstLog.status.equals("Removed")) {
			flRemovedDate = firstLog.lastTouch;
		}

		LogParser currLog;
		LogParser nextLog;
		for(int i = 0; i < lpList.size() - 1; i++) {
			currLog = lpList.get(i);
			nextLog = lpList.get(i+1);
			
			if(!currLog.status.equals(nextLog.status)) {
				if(nextLog.status.equals("Transplanted")) {
					flTranspDate = nextLog.lastTouch;
				} else if(nextLog.status.equals("Inactive")) {
					flInactiveDate = nextLog.lastTouch;
				} else if(nextLog.status.equals("Removed")) {
					flRemovedDate = nextLog.lastTouch;
				}
			}
		}
		
		LogParser lastLog = lpList.get(lpList.size()-1);
		
		String result = epochToDate(flTranspDate) + "," + epochToDate(flInactiveDate) + "," + epochToDate(flRemovedDate) + "," + lastLog.status + "," + epochToDate(lastLog.lastTouch);
		return result;
	}
	
	public static void main (String[] args) {
		
		HashMap<String, ArrayList<LogParser>> dbase = new HashMap<String, ArrayList<LogParser>>();
    	String fileName = "SystemLog.txt";
		try {
	        BufferedReader rd = new BufferedReader(new FileReader(fileName));
	        String line;
	        String dbquery = "";
//	        int count = 0;
	        // Read in each line
	        while ((line = rd.readLine()) != null) {
	        	
	        	if(line.length() > 0) {
	        		dbquery += line;
	        		
	        	} else if(dbquery.length() > 0){
	        		
	        		if(dbquery.contains(UPDATE_STR) && !dbquery.contains(UPDATE_ADLIST_STR) && !dbquery.contains(UPDATE_ANTIBODIES_STR) && !dbquery.contains(UPDATE_USER_STR) && !getPersonType(dbquery).equals("user") && !getPatientID(dbquery).equals("")) {
		        		LogParser log = new LogParser(getPatientID(dbquery), getPersonType(dbquery), getStatus(dbquery), getLastTouch(dbquery));
		        		
		        		ArrayList<LogParser> entries;
		        		if(dbase.containsKey(log.patientID)) {
		        			entries = dbase.get(log.patientID);
		        		} else {
		        			entries = new ArrayList<LogParser>();
		        		}
	        			entries.add(log);
		        		dbase.put(log.patientID, entries);
	        		}
	        		dbquery = "";
	        	}	
//	        	count++;
	        }
	        rd.close();
	        
	    } catch (IOException ex) {
	    	System.err.println("Reading error");
	    }

	    // Print database
	    for(String id : dbase.keySet()) {
	    	for(LogParser log : dbase.get(id)) {
	    		System.out.println(log.patientID + " " + log.personType + " " + log.status + " " + log.lastTouch);
	    	}
	    }
	    
	    /* PrintWriter */
	    String outputFile = "allDatabaseUpdates.csv";
	    try {
            PrintWriter wr = new PrintWriter(new FileWriter(outputFile));
            
            for(String id : dbase.keySet()) {
    	    	for(LogParser log : dbase.get(id)) {
    	    		wr.println(log.patientID + "," + log.personType + "," + log.status + "," + epochToDate(log.lastTouch));
    	    	}
    	    }
            
            wr.close();
        } catch (IOException ex) {
            System.err.println("Writing error.");
        }
        
        
        /* Process */
	    System.out.println("Processing...");
//	    System.out.println(getStatusDate(dbase.get("136612")));
	    String uniqueStatusFile = "uniqueStatusUpdates.csv";
	    try {
            PrintWriter wr = new PrintWriter(new FileWriter(uniqueStatusFile));
            wr.println("id,transplanted_date,inactive_date,removed_date,last_status,last_status_update");
            for(String id : dbase.keySet()) {
	    		wr.println(id + "," + getStatusDate(dbase.get(id)));
    	    }
            
            wr.close();
        } catch (IOException ex) {
            System.err.println("Writing error.");
        }
	    System.out.println("Finished writing...");
	    
	}

}
