package com.mkyong.quartz;

import java.sql.Connection;
import java.util.StringTokenizer;
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
//
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
		 * 		SYNC USERS FROM DB2		*
		 *******************************/

		// Check disable get data from db2
		String eableIncoming = synDB.getConfigProperties("ENABLE_DB2_DATA");
		if (eableIncoming.equalsIgnoreCase("Y")) {

			lsSTGUsers = getCustomStgUsers();

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
							+ row[11] + ", CREATED_DATE :" + row[12]);
					String id_nric = (String) row[0];
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
					String user__sub_type = (String) row[11];
					System.out.println("Check user exist in user entity table");
					if (synDB.checkExistUserEntity(id_nric) == 0) {
						System.out.println("======================");
						System.out
								.println("Insert into UserEntity: " + id_nric);
						
						//Insert to User Entity table
						synDB.insertToUserEntity(id_nric, first_name,
								last_name, mobile, email, account_status,
								agent_code, agency, need2fa, needtnc);
					}

					if (synDB.checkExistStgUser(id_nric) == 0) {
						noOfRecordInputUser += 1;

						synDB.insertSTGUser(id_nric, first_name, last_name,
								email, account_status, mobile, agent_code,
								agency, need2fa, needtnc, user_type,
								user__sub_type);
					}
				}
			}

			sNoOfRecordInputUser += noOfRecordInputUser + sNewLine;

			List<Object[]> lsSTGUserRoles = getCustomStgUserRoles();
			for (Object[] row : lsSTGUserRoles) {
				String id_nric = (String) row[0];
				String role_name = (String) row[1];
				if (synDB.checkExistStgUserRole(id_nric, role_name) == 0) {
					System.out.println("INSERT TO KEYCLOAK | ID_NRIC:" + row[0]
							+ ", ROLE_NAME:" + row[1]);
					synDB.insertSTGUserRole(id_nric, role_name);
				}
			}
		}
		
		
		

		/**
		 * Check disable sync to keycloak db
		 */
		String eableOutgoing = synDB
				.getConfigProperties("ENABLE_KEYCLOAK_DATA");
		System.out.println("eableOutgoing: " + eableOutgoing);
		if (eableOutgoing.equalsIgnoreCase("N")) {
			return;
		}
		
		
		/*******************************************************************
		 * 			SYNC USER ROLES into KEYCLOAK_ROLE (ROLE MASTER TABLE) *
		 *******************************************************************/
		
		List<Object[]> lsNewStgRoles = synDB.getNewRolesFromStg();
		List<String> newRoleIds = new ArrayList<String>();
		
		//Get PSE Realm ID
		List<Object[]> lsRealmIDs = synDB.getPSERealmId();
		if (lsRealmIDs.size() > 0) {
			String realmId = "";
			String roleName = "";
			for (Object[] row : lsRealmIDs) {
				realmId = (String) row[0];
				break;
			}

			sNoOfRecordInputRole += lsNewStgRoles.size() + sNewLine;
			if (lsNewStgRoles.size() > 0 && !realmId.equals("")) {
				for (Object row : lsNewStgRoles) {
					roleName = (String) row;
					if (roleName != null && !roleName.equals("")) {
						String id = genearateUDID();
						newRoleIds.add(id);
						if(roleName.trim().contains(",")){
							String[] roleNames = roleName.trim().split(",");
							for (String role : roleNames) {
								synDB.insertKeycloakRole(id, realmId,
										false, role, realmId, null, realmId, false);
								countRoleProcessed += 1;
							}
						}else{
							synDB.insertKeycloakRole(id, realmId,
									false, roleName, realmId, null, realmId, false);
							countRoleProcessed += 1;
						}
					}
				}

				countRoleNotProcessed = lsNewStgRoles.size()
						- countRoleProcessed;
			}
		}
		


		
		/******************************************************************
		 * 		SYNC USER ROLES INTO USER_ROLE_MAPPING (ASSIGN USER ROLE) *
		 ******************************************************************/

		List<Object[]> lsSTGUserRoles = getCustomStgUserRoles();
		for (Object[] row : lsSTGUserRoles) {
			String id_nric = (String) row[0];
			String role_name = (String) row[1];
			String userId = synDB.getNewUserId(id_nric.trim());
			String role_id = synDB.getRoleIdByName(role_name.trim());
			System.out.println("Assign role ["+role_id+"] for [" + username+"]");
			if(!userId.trim().equals("") && !role_id.trim().equals(""))
				synDB.insertUserRoleMapping(role_id, userId);
		}
		
		
		/***
		 * Update custom_user
		 */

		synDB.insertToCustomUser();

		
		/********************************************
		 * 		SYNC USER TYPE AND USER SUBTYPE		*
		 *******************************************/
		
		List<Object[]> lsUserTypeNotMapping = synDB.checkUserTypeNotMapping();
		System.out.println("Get user type not mapping count: "
				+ lsUserTypeNotMapping.size());

		StringBuilder sbUserTypeNotInsert = null;
		for (Object[] row : lsUserTypeNotMapping) {
			String user_type = (String) row[0];
			String user_sub_type = (String) row[1];
			System.out.println("Get user_type:" + user_type
					+ " - user_sub_type:" + user_sub_type);
			String userTypeId = genearateUDID();

			int result = synDB.insertCustomUserType(user_type, userTypeId);

			String userSubTypeId = genearateUDID();
			if (result != -1) {
				System.out.println("User type " + user_type + " is exist!");
				synDB.insertCustomUserSubType(user_sub_type, userSubTypeId,
						userTypeId);
				System.out.println("Custom user sub type id:" + userSubTypeId);
			} else {
				// Send to admin the user type not exist
				sbUserTypeNotInsert = new StringBuilder();
				sbUserTypeNotInsert.append(user_type).append("; ");
			}
		}

		List<Object[]> lsUserSubTypeNotMapping = synDB
				.checkUserSubTypeNotMapping();
		System.out.println("Get user sub type not mapping count: "
				+ lsUserTypeNotMapping.size());
		for (Object[] row : lsUserSubTypeNotMapping) {
			String user_sub_type = (String) row[0];
			String user_type = (String) row[1];
			System.out.println("Get user_type: - user_sub_type:"
					+ user_sub_type);
			String userSubTypeId = genearateUDID();
			String userTypeId = synDB.getUserTypeId(user_type);
			System.out.println("Custom user type id:" + userTypeId);
			if (!userTypeId.equals("")) {
				synDB.insertCustomUserSubType(user_sub_type, userSubTypeId,
						userTypeId);
				System.out.println("Custom user sub type id:" + userSubTypeId);
			}
		}


		/********************************************
		 * UPDATE USER TYPE, SUB TYPE FOR NEW USER	*
		 *******************************************/

		List<Object[]> lsUserTypeUserNames = synDB.getListUserTypesUserNames();
		for (Object[] row : lsUserTypeUserNames) {
			String username = (String) row[0];
			String userType = (String) row[1];
			String userSubType = (String) row[2];
			String userTypeId = synDB.getUserTypeId(userType);
			String userSubTypeId = synDB.getUserSubTypeId(userSubType);
			if (!userTypeId.equals("")) {
				countUserProcessed += 1;
				synDB.updateUserTypeSubTypeForUserEntity(userTypeId,
						userSubTypeId, username);
			}
		}

		countUserNotProcessed = noOfRecordInputUser - countUserProcessed;
		

		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		String sUserTypeNotExist = "";
		if (sbUserTypeNotInsert != null) {
			sUserTypeNotExist = "UserType not exist: "
					+ sbUserTypeNotInsert.toString() + sNewLine;
		}
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

	private List<Object[]> getCustomStgUsers() {
		List<Object[]> listData = new ArrayList<Object[]>();

		String ServerName = synDB.getConfigProperties("SERVER_NAME");
		int PortNumber = Integer.parseInt(synDB
				.getConfigProperties("PORT_NUMBER"));
		String DatabaseName = synDB.getConfigProperties("DATABASE");

		java.util.Properties properties = new java.util.Properties();

		properties.put("user", synDB.getConfigProperties("USER"));
		properties.put("password", synDB.getConfigProperties("PASSWORD"));
		properties.put("sslConnection", "true");
		System.setProperty("javax.net.ssl.trustStore",
				synDB.getConfigProperties("CACERTS_PATH"));
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		System.setProperty("db2.jcc.charsetDecoderEncoder", "3");
		String url = "jdbc:db2://" + ServerName + ":" + PortNumber + "/"
				+ DatabaseName;

		java.sql.Connection con = null;
		try {
			new DB2Driver();
		} catch (Exception e) {
			System.out.println("Error: failed to load Db2 jcc driver.");
		}

		try {
			System.out.println("url: " + url);
			con = java.sql.DriverManager.getConnection(url, properties);
			try {
				String sql = synDB.getConfigProperties("SQL_CUSTOM_STG_USER");
				StringBuilder sbSQLSTGUser = new StringBuilder();
				sbSQLSTGUser.append(sql);

				System.out.println("Select from PSE.CUSTOM_STG_USER DB2: "
						+ sbSQLSTGUser.toString());
				java.sql.Statement ps = con.createStatement();
				java.sql.ResultSet rs = ps
						.executeQuery(sbSQLSTGUser.toString());
				System.out.println("get data....");

				Object[] objArrays = new Object[13];
				while (rs.next()) {
					objArrays = new Object[13];
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
							+ ", CREATED_DATE :" + objArrays[12]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("select is failing1");
			}

			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return listData;
	}

	private List<Object[]> getCustomStgUserRoles() {
		List<Object[]> listData = new ArrayList<Object[]>();

		String ServerName = synDB.getConfigProperties("SERVER_NAME");
		int PortNumber = Integer.parseInt(synDB
				.getConfigProperties("PORT_NUMBER"));
		String DatabaseName = synDB.getConfigProperties("DATABASE");

		java.util.Properties properties = new java.util.Properties();

		properties.put("user", synDB.getConfigProperties("USER"));
		properties.put("password", synDB.getConfigProperties("PASSWORD"));
		properties.put("sslConnection", "true");
		System.setProperty("javax.net.ssl.trustStore",
				synDB.getConfigProperties("CACERTS_PATH"));
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		System.setProperty("db2.jcc.charsetDecoderEncoder", "3");
		String url = "jdbc:db2://" + ServerName + ":" + PortNumber + "/"
				+ DatabaseName;

		java.sql.Connection con = null;
		try {
			new DB2Driver();
		} catch (Exception e) {
			System.out.println("Error: failed to load Db2 jcc driver.");
		}

		try {
			System.out.println("url: " + url);
			con = java.sql.DriverManager.getConnection(url, properties);
			try {
				String sql = synDB
						.getConfigProperties("SQL_CUSTOM_STG_USER_ROLE");
				StringBuilder sbSQLSTGUser = new StringBuilder();
				sbSQLSTGUser.append(sql);

				System.out
						.println("Select from PSE.SQL_CUSTOM_STG_USER_ROLE DB2: "
								+ sbSQLSTGUser.toString());
				java.sql.Statement ps = con.createStatement();
				java.sql.ResultSet rs = ps
						.executeQuery(sbSQLSTGUser.toString());
				System.out.println("get data....");

				Object[] objArrays = new Object[3];
				while (rs.next()) {
					objArrays = new Object[3];
					objArrays[0] = rs.getString(1);
					objArrays[1] = rs.getString(2);
					objArrays[2] = rs.getString(3);
					listData.add(objArrays);
					System.out.println("GET FROM DB2 | ID_NRIC:" + objArrays[0]
							+ ", ROLE_NAME:" + objArrays[1] + ", CREATED_DATE:"
							+ objArrays[2]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("select is failing1");
			}

			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return listData;
	}
	
	public static void main(String args[]) {
	
	}
}
