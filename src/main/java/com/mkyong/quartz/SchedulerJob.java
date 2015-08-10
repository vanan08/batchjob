package com.mkyong.quartz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import com.ibm.db2.jcc.DB2Driver;
import com.mkyong.service.SynDB;
import com.prunation.mail.Mail;
import java.util.*;
public class SchedulerJob extends QuartzJobBean {

	private SynDB synDB;
	Connection connection = null;
	PreparedStatement statement = null;
	ResultSet rs = null;
	Mail mailBox;

	String host = "";
	String port = "";
	String password = "";
	String emailFrom = "";
	String emailTo = "";
	String subject = "";
	String username = "";
	String logfilePath = "";

	String sNewLine = "\n";
	String sNoOfRecordInputUser = "No of user rows (Input): ";
	String sNoOfRecordPressedUser = "No of user records (Processed): ";
	String sNoOfRecordNotPressedUser = "No of user records (Not Processed): ";
	String sNoOfRecordInputRole = "No of role rows (Input): ";
	String sNoOfRecordPressedRole = "No of role records (Processed): ";
	String sNoOfRecordNotPressedRole = "No of role records (Not Processed): ";
	String sProcessDuration = "ProcessDuration: ";
	String sRemarkError = "Remark/Error: ";

	int countUserProcessed = 0;
	int countUserNotProcessed = 0;
	int countRoleProcessed = 0;
	int countRoleNotProcessed = 0;
	int noOfRecordInputUser = 0;


	protected void executeInternal(JobExecutionContext context)
			throws JobExecutionException {

		List<Object[]> lsSTGUsers = new ArrayList<Object[]>();
		synDB = (SynDB) context.getMergedJobDataMap().get("synDB");

		String eable = synDB.getConfigProperties("ENABLE_BATCH_JOB");
		if (eable.equalsIgnoreCase("N")) {
			return;
		}

		long startTime = System.currentTimeMillis();

		System.out.println("Batch Job running at "
				+ Calendar.getInstance().getTime());

		host = synDB.getConfigProperties("SMTP_HOST");
		port = synDB.getConfigProperties("SMTP_PORT");
		password = synDB.getConfigProperties("SMTP_PASSWORD");
		emailFrom = synDB.getConfigProperties("FROM");
		emailTo = synDB.getConfigProperties("TO");
		subject = synDB.getConfigProperties("SUBJECT");
		username = synDB.getConfigProperties("SMTP_USER");
		logfilePath = synDB.getConfigProperties("LOG_FILE");

		
		/**
		 * Check disable sync to keycloak db
		 */
		String eableOutgoing = synDB
				.getConfigProperties("ENABLE_KEYCLOAK_DATA");
		System.out.println("eableOutgoing: " + eableOutgoing);
		if (eableOutgoing.equalsIgnoreCase("N")) {
			return;
		}
		
		/**
		 * Clear user type, subtype
		 */
		String clearUserType = synDB
				.getConfigProperties("ENABLE_CLEAR_USER_TYPE");
		if (clearUserType.equalsIgnoreCase("Y")) {
			synDB.clearUserTypeSubType();
			synDB.deleteSubType();
			synDB.deleteUserType();
		}

		/********************************
		 * 		SYNC USERS          	*
		 *******************************/

		// Check disable get data from db2
		String eableIncoming = synDB.getConfigProperties("ENABLE_DB2_DATA");
		if (eableIncoming.equalsIgnoreCase("Y")) {

			lsSTGUsers = getCustomStgUsers(false);

			if (lsSTGUsers != null) {
				System.out.println("Sync to CUSTOM_STG_USER in keycloak");
				for (Object[] row : lsSTGUsers) {
					System.out.println("INSERT TO KEYCLOAK | ID_NRIC:" + row[0]
							+ ", FIRST_NAME:" + row[1] + ", LAST_NAME:"
							+ row[2] + ", MOBILE:" + row[3] + ", EMAIL:"
							+ row[4] + ", ACCOUNT_STATUS:" + row[5]
							+ ", AGENT_CODE:" + row[6] + ", AGENCY:" + row[7]
							+ ", NEED2FA:" + row[8] + ", NEEDTNC:" + row[9]
							+ ", USER_TYPE:" + row[10] + ", USER_SUB_TYPE:"
							+ row[11] + ", CREATED_DATE :" + row[12]
					        + row[12] + ", ROLE_NAME :" + row[13]);
					
					String id_nric = (String) row[0];
					if(id_nric!=null) {
						id_nric=id_nric.toUpperCase().trim();
					}
					
//					a.ID_NRIC, FIRST_NAME, LAST_NAME, MOBILE, EMAIL, ACCOUNT_STATUS, AGENT_CODE, 
//					AGENCY, NEED2FA, NEEDTNC, USER_TYPE, USER__SUB_TYPE, a.CREATED_DATE, b.ROLE_NAME
					String first_name = (String) row[1];
					String last_name = (String) row[2];
					String mobile = (String) row[3];
					String email = (String) row[4];
					String account_status = (String) row[5];
					String agent_code = (String) row[6];
					String agency = (String) row[7];
					String need2fa = (String) row[8];
					String needtnc = (String) row[9];
					String user_type = (String) row[10];
					if(user_type!=null) {
						user_type=user_type.toUpperCase().trim();
					}
					String user_sub_type = (String) row[11];
					if(user_sub_type!=null) {
						user_sub_type=user_type.toUpperCase().trim();
					}
					String roles = (String) row[13];
					System.out.println("roles: "+roles);
					//check for user type
					String userTypeId = synDB.getUserTypeId(user_type);
					if(userTypeId.trim().equals("")) {
						userTypeId = genearateUDID();
						synDB.insertCustomUserType(user_type, userTypeId);
					}
					
					//check for sub user type
					String userSubTypeId = synDB.getUserSubTypeId(user_sub_type);
					if(userSubTypeId.trim().equals("")) {
						userSubTypeId = genearateUDID();
						synDB.insertCustomUserSubType(user_sub_type, userSubTypeId,userTypeId);
					}
					else {
						synDB.updateCustomUserSubType(user_sub_type, userSubTypeId,userTypeId);
					}
					
					System.out.println("userSubTypeId: "+userSubTypeId);
					System.out.println("userTypeId: "+userTypeId);
					
					System.out.println("Check user exist in user entity table");
					String user_id=synDB.getUserEntityByUsername(id_nric);
					if (user_id.trim().equals("")) {
						System.out.println("======================");
						System.out
								.println("Insert into UserEntity: " + id_nric);
						
						//Insert to User Entity table
						synDB.insertToUserEntity(id_nric, first_name,
								last_name, mobile, email, account_status,
								agent_code, agency, need2fa, needtnc, userTypeId, userSubTypeId);
					}
					else {
						synDB.updateUserEntityByID(first_name, last_name, mobile, email, account_status, agent_code, agency, userTypeId, userSubTypeId, user_id);
					}
					
					//manage app role
					List<String> lsUserTypeRoles = synDB.getRolesInUserType(user_type);
					for (String rw : lsUserTypeRoles) {
						//add user_role_mapping by user_id and role_id (app role set in user type)
						String role_id = rw;
						System.out.println("========AND=====role_id: "+role_id);
						//delete record by user_id and role_id in user_role_mapping 
						if(role_id != null && !role_id.trim().equals("")){
							synDB.deleteUserRoleMappingByUserIdRoleId(user_id,role_id);
							
							//add user_role_mapping by user_id and role_id (user role)
							synDB.insertUserRoleMapping(user_id, role_id);
						}
					}	
					
					//manage user role
					StringTokenizer st = new StringTokenizer(roles,",");
					while(st.hasMoreElements()) {
						String role = (String) st.nextElement();
						if(role!=null) {
							role = role.trim().toUpperCase();
						}
						
						//sync keycloak_role by role_name
						String keycloak_role_id = synDB.getKeycloakRoleByName(role);
						if (keycloak_role_id.trim().equals("")) {
							keycloak_role_id = genearateUDID();
							String realmId = synDB.getPSERealmId();
							
							synDB.insertKeycloakRole(keycloak_role_id, realmId,
									false, role, realmId, null, realmId, false);
							countRoleProcessed += 1;
						}
						System.out.println("========AND=====keycloak_role_id: "+keycloak_role_id);
						//delete record by user_id and role_id in user_role_mapping 
						synDB.deleteUserRoleMappingByUserIdRoleId(user_id,keycloak_role_id);

						//add user_role_mapping by user_id and role_id (user role)
						synDB.insertUserRoleMapping(user_id, keycloak_role_id);
					}
				}
			}

			sNoOfRecordInputUser += noOfRecordInputUser + sNewLine;
		}
		
		countUserNotProcessed = noOfRecordInputUser - countUserProcessed;
		

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		String sUserTypeNotExist = "";
		
		/*
		if (sbUserTypeNotInsert != null) {
			sUserTypeNotExist = "UserType not exist: "
					+ sbUserTypeNotInsert.toString() + sNewLine;
		}*/
		
		String content = sNoOfRecordInputUser + sNoOfRecordPressedUser
				+ sNoOfRecordNotPressedUser;
		content += sNoOfRecordInputRole + sNoOfRecordPressedRole
				+ sNoOfRecordNotPressedRole;
		content += sUserTypeNotExist;
		content += "Process duration: " + totalTime + "(millisecond)";
		String enableEmail = synDB.getConfigProperties("ENABLE_EMAIL");

		// Check disable send mail
		if (enableEmail.equalsIgnoreCase("Y")) {
			try {
				System.out.println("Sending email...");
				Mail mailBox = new Mail(emailFrom, emailTo, subject, host,
						port, username, password, content, logfilePath);
				mailBox.send();
				System.out.println("Mail sent");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("totalTime: " + totalTime);

	}
 

	public static String genearateUDID() {
		char[] chars = "abcdefghijklmnopqrstuvwxyzABSDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
				.toCharArray();
		Random r = new Random(System.currentTimeMillis());
		char[] id = new char[36];
		for (int i = 0; i < 36; i++) {
			id[i] = chars[r.nextInt(chars.length)];
		}
		return new String(id);
	}

	private List<Object[]> getCustomStgUsers(boolean usedb2) {
		List<Object[]> listData = new ArrayList<Object[]>();
		String ServerName = "";
		int PortNumber;
		String DatabaseName = "";
		java.util.Properties properties;
		String url = "";
		java.sql.Connection con = null;

		if(usedb2) {
			ServerName = synDB.getConfigProperties("SERVER_NAME");
			PortNumber = Integer.parseInt(synDB
					.getConfigProperties("PORT_NUMBER"));
			DatabaseName = synDB.getConfigProperties("DATABASE");
			properties = new java.util.Properties();
			properties.put("user", synDB.getConfigProperties("USER"));
			properties.put("password", synDB.getConfigProperties("PASSWORD"));
			properties.put("sslConnection", "true");
			System.setProperty("javax.net.ssl.trustStore",
					synDB.getConfigProperties("CACERTS_PATH"));
			System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
			System.setProperty("db2.jcc.charsetDecoderEncoder", "3");
			url = "jdbc:db2://" + ServerName + ":" + PortNumber + "/"
					+ DatabaseName;

			try {
				new DB2Driver();
			} catch (Exception e) {
				System.out.println("Error: failed to load Db2 jcc driver.");
			}
		}
		else {
			ServerName = synDB.getConfigProperties("SERVER_NAME");
			PortNumber = Integer.parseInt(synDB
					.getConfigProperties("PORT_NUMBER"));
			DatabaseName = synDB.getConfigProperties("DATABASE");
			
			properties = new java.util.Properties();
			properties.put("user", synDB.getConfigProperties("USER"));
			properties.put("password", synDB.getConfigProperties("PASSWORD"));
			url = "jdbc:postgresql://" + ServerName + ":" + PortNumber + "/"
					+ DatabaseName;
		}
		
		try {
			System.out.println("url: " + url);
			con = java.sql.DriverManager.getConnection(url, properties);
			try {
				String sql = "SELECT a.ID_NRIC, FIRST_NAME, LAST_NAME, MOBILE, EMAIL, ACCOUNT_STATUS, AGENT_CODE, AGENCY, NEED2FA, NEEDTNC, USER_TYPE, USER__SUB_TYPE, a.CREATED_DATE, b.ROLE_NAME " 
						   + "FROM CUSTOM_STG_USER a INNER JOIN CUSTOM_STG_USER_ROLE b ON (b.ID_NRIC=a.ID_NRIC) ";
				StringBuilder sbSQLSTGUser = new StringBuilder();
				sbSQLSTGUser.append(sql);

				System.out.println("Select from PSE.CUSTOM_STG_USER DB2: "
						+ sbSQLSTGUser.toString());
				java.sql.Statement ps = con.createStatement();
				java.sql.ResultSet rs = ps
						.executeQuery(sbSQLSTGUser.toString());
				System.out.println("get data....");

				Object[] objArrays = new Object[14];
				while (rs.next()) {
					objArrays = new Object[14];
					objArrays[0] = rs.getString(1);
					objArrays[1] = rs.getString(2);
					objArrays[2] = rs.getString(3);
					objArrays[3] = rs.getString(4);
					objArrays[4] = rs.getString(5);
					objArrays[5] = rs.getString(6);
					objArrays[6] = rs.getString(7);
					objArrays[7] = rs.getString(8);
					objArrays[8] = rs.getString(9);
					objArrays[9] = rs.getString(10);
					objArrays[10] = rs.getString(11);
					objArrays[11] = rs.getString(12);
					objArrays[12] = rs.getString(13);
					objArrays[13] = rs.getString(14);
					listData.add(objArrays);
					System.out.println("GET FROM DB2 | ID_NRIC:" + objArrays[0]
							+ ", FIRST_NAME:" + objArrays[1] + ", LAST_NAME:"
							+ objArrays[2] + ", MOBILE:" + objArrays[3]
							+ ", EMAIL:" + objArrays[4] + ", ACCOUNT_STATUS:"
							+ objArrays[5] + ", AGENT_CODE:" + objArrays[6]
							+ ", AGENCY:" + objArrays[7] + ", NEED2FA:"
							+ objArrays[8] + ", NEEDTNC:" + objArrays[9]
							+ ", USER_TYPE:" + objArrays[10]
							+ ", USER_SUB_TYPE:" + objArrays[11]
							+ ", CREATED_DATE :" + objArrays[12]
					        + ", ROLE_NAME :" + objArrays[13]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("select is failing1");
			}

			
			if(con!=null) {
				try {
				  con.close();
				  con = null;
				}
				catch(Exception e) {}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return listData;
	}
}
