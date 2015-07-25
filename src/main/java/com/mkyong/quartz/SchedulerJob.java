package com.mkyong.quartz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.ibm.db2.jcc.DB2Driver;
import com.mkyong.service.SynDB;

public class SchedulerJob extends QuartzJobBean {

	private SynDB synDB;
	Connection connection = null;
	PreparedStatement statement = null;
	ResultSet rs = null;

	protected void executeInternal(JobExecutionContext context)
			throws JobExecutionException {
		int user_entity_row_inserted = 0;

		System.out.println("Batch Job running at "
				+ Calendar.getInstance().getTime());

		synDB = (SynDB) context.getMergedJobDataMap().get("synDB");

		String eable = synDB.getEnableProperties("enable");
		if (eable.equalsIgnoreCase("N")) {
			System.out.println("Batch Job is turn off...");
			return;
		}

		/***
		 * Sync DB2
		 */
		// getDataFromDB2();
		//
		// String oldCreatedDateSTGUser = synDB.getMaxCreatedDateSTGUser();
		// String oldCreatedDateSTGUserRole = synDB
		// .getMaxCreatedDateStgUserRoles();
		//
		// if (oldCreatedDateSTGUser != null) { oldCreatedDateSTGUser =
		// "1970-01-01 :00:00:00"; } if (oldCreatedDateSTGUserRole != null) {
		// oldCreatedDateSTGUserRole = "1970-01-01 :00:00:00"; } if (synODSDB !=
		// null) { List<Object[]> lsSTGUsers =
		// getCustomStgUsers(oldCreatedDateSTGUser); for (Object[] row :
		// lsSTGUsers) { String id_nric = (String) row[0]; String first_name =
		// (String) row[1]; String last_name = (String) row[2]; String email =
		// (String) row[3]; String account_status = (String) row[4]; String
		// mobile = (String) row[5]; String agent_code = (String) row[6]; String
		// agency = (String) row[7]; String need2fa = (String) row[8]; String
		// needtnc = (String) row[9]; String user_type = (String) row[10];
		// String user__sub_type = (String) row[11];
		// synDB.insertSTGUser(id_nric, first_name, last_name, email,
		// account_status, mobile, agent_code, agency, need2fa, needtnc,
		// user_type, user__sub_type); }
		//
		// List<Object[]> lsSTGUserRoles =
		// getCustomStgUserRoles(oldCreatedDateSTGUserRole);
		//
		// for (Object[] row : lsSTGUserRoles) { String id_nric = (String)
		// row[0]; String role_name = (String) row[1];
		// synDB.insertSTGUserRole(id_nric, role_name); } }
		/***
		 * TODO: Get all new user
		 */
		List<Object[]> lsNewUsers = synDB.getNewUsers();
		/***************
		 * update user_entity
		 */

		int new_rows = synDB.countNewRows();
		System.out.println("Get new rows count: " + new_rows);
		if (new_rows > 0) {
			System.out.println("Migrate User Entity");
			user_entity_row_inserted = synDB.migrateUserEntity();
			System.out.println("Migrate User Entity >> Num Rows inserted: "
					+ user_entity_row_inserted);
		}

		/*****
		 * Get all user id just inserted belog new list user name get above
		 */
		for (Object[] row : lsNewUsers) {
			String username = (String) row[0];
			String userType = (String) row[1];
			String userId = synDB.getNewUserId(username);
			System.out.println("Custom user id:" + userId);
			System.out.println("Custom user userType:" + userType);
			/*******
			 * Get roles id from role list names get from config file
			 */

			List<String> rolesList = synDB.getUserTypeRoles(userType);

			for (String _role_id : rolesList) {
				System.out.println("Get configration role id:" + _role_id);
				/*********
				 * Insert to user_role_mapping
				 */
				if (!_role_id.equals("")) {
					synDB.insertUserRoleMapping(_role_id, userId);
				} else {
					System.out.println("Can't find role id '" + _role_id
							+ "' from keycloak_role table.");
				}
			}
		}

		/***
		 * Update custom_user
		 */

		synDB.insertToCustomUser();

		/*******
		 * Check user type not exist
		 */
		List<Object[]> lsUserTypeNotMapping = synDB.checkUserTypeNotMapping();
		System.out.println("Get user type not mapping count: "
				+ lsUserTypeNotMapping.size());
		for (Object[] row : lsUserTypeNotMapping) {
			String user_type = (String) row[0];
			String user_sub_type = (String) row[1];
			System.out.println("Get user_type:" + user_type
					+ " - user_sub_type:" + user_sub_type);
			String userTypeId = genearateUDID();
			synDB.insertCustomUserType(user_type, userTypeId);
			String userSubTypeId = genearateUDID();
			synDB.insertCustomUserSubType(user_sub_type, userSubTypeId,
					userTypeId);
			System.out.println("Custom user sub type id:" + userSubTypeId);
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

		/******
		 * Listing stg role
		 */

		List<Object[]> lsNewStgRoles = synDB.getNewRolesFromStg();

		/******
		 * Get list application
		 */
		List<Object[]> lsAppsOfRealm = synDB.getListAppsBelongRealm();

		/******
		 * Get PSE Realm ID
		 */
		List<Object[]> lsRealmIDs = synDB.getPSERealmId();
		if (lsRealmIDs.size() > 0) {
			String realmId = "";
			String roleName = "";
			String applicationId = "";

			for (Object[] row : lsRealmIDs) {
				realmId = (String) row[0];
				break;
			}

			if (lsNewStgRoles.size() > 0 && !realmId.equals("")) {
				for (Object row : lsNewStgRoles) {
					roleName = (String) row;
					if (!roleName.equals("")) {
						for (Object appRow : lsAppsOfRealm) {
							applicationId = (String) appRow;
							if (!applicationId.equals("")) {
								String id = genearateUDID();
								synDB.insertKeycloakRole(id, applicationId,
										true, roleName, realmId, applicationId);
							}
						}
					}
				}
			}
		}

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
		String ServerName = "10.163.170.78";
		int PortNumber = 55702;
		String DatabaseName = "ODSSG";

		java.util.Properties properties = new java.util.Properties();

		properties.put("user", "raisapp");
		properties.put("password", "password");
		properties.put("sslConnection", "true");
		System.setProperty("javax.net.ssl.trustStore",
				"/jboss/jboss-eap-6.3/cacerts");
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
				String sql = "SELECT ID_NRIC, FIRST_NAME, LAST_NAME FROM PSE.CUSTOM_STG_USER ";
				// + "WHERE CREATED_DATE > "
				// + "date(to_date(?,'DD-MM-YYYY HH:MI:SS'));";

				java.sql.Statement ps = con.createStatement();
				System.out.println("select is executeQuery");
				java.sql.ResultSet rs = ps.executeQuery(sql);
				System.out.println("get data....");
				while (rs.next()) {
					System.out.println(rs.getString(1));
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("select is failing1");
			}

			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	private List<Object[]> getCustomStgUserRoles() {
		return null;
	}
}
