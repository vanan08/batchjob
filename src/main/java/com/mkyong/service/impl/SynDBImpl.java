package com.mkyong.service.impl;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import com.mkyong.service.SynDB;

@Transactional
public class SynDBImpl implements SynDB, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Autowired
	@Qualifier("sessionFactory")
	public SessionFactory sessionFactory;

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public int insertSTGUser(String id_nric, String first_name,
			String last_name, String email, String account_status,
			String mobile, String agent_code, String agency, String need2fa,
			String needtnc, String user_type, String user__sub_type) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"insertSTGUser");
		sqlQuery.setParameter("id_nric", id_nric);
		sqlQuery.setParameter("first_name", first_name);
		sqlQuery.setParameter("last_name", last_name);
		sqlQuery.setParameter("email", email);
		sqlQuery.setParameter("email", email);
		sqlQuery.setParameter("mobile", mobile);
		sqlQuery.setParameter("agent_code", agent_code);
		sqlQuery.setParameter("agency", agency);
		sqlQuery.setParameter("need2fa", need2fa);
		sqlQuery.setParameter("needtnc", needtnc);
		sqlQuery.setParameter("user_type", user_type);
		sqlQuery.setParameter("user__sub_type", user__sub_type);
		
		System.out.println("Insert new stg user '" + id_nric );
		return sqlQuery.executeUpdate();
	}
	
	public int insertSTGUserRole(String id_nric, String role_name) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"insertSTGUser");
		sqlQuery.setParameter("id_nric", id_nric);
		sqlQuery.setParameter("role_name", role_name);
		
		System.out.println("Insert new stg user role '" + role_name+ "'");
		return sqlQuery.executeUpdate();
	}

	public String getMaxCreatedDateSTGUser() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getMaxCreatedDateSTGUser");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		for (Object[] row : returnList) {
			if(row != null && row[0] != null){
				String createdDate = (String) row[0];
				return createdDate;
			}
		}
		System.out.println("Can not found created date in STG User table");
		return null;

	}

	public String getMaxCreatedDateStgUserRoles() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getMaxCreatedDateSTGUserRole");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		for (Object[] row : returnList) {
			if(row != null && row[0] != null){
				String createdDate = (String) row[0];
				return createdDate;
			}
		}
		System.out.println("Can not found created date in STG User table");
		return null;
	}

	public List<Object[]> getUserRealm() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getUserRealm");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		return returnList;
	}

	public void query() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"get_document");
		List<Object> returnList = sqlQuery.list();
		System.out.println("Return list size : " + returnList.size());
	}

	public List<String> getUserTypeRoles(String userType) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getUserTypeRoles");
		String user_type = "";
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		List<String> returnFilterList = new ArrayList<String>();
		for (Object[] row : returnList) {
			String role_id = (String) row[1];
			user_type = (String) row[2];
			if (user_type.equalsIgnoreCase(userType)) {
				returnFilterList.add(role_id);
			}
		}
		return returnFilterList;
	}

	public String getRoleId(String rolename) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getRoleId");
		// sqlQuery.setParameter("name", rolename);
		String role_id = "";
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		for (Object[] row : returnList) {
			role_id = (String) row[0];
			String role_name = (String) row[1];
			if (role_name.equals("")) {
				System.out.println("role id:" + role_id);
				break;
			}
		}
		return role_id;
	}

	public boolean getUserRoleMapping(String roleId, String userId) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getUserRoleMapping");
		String role_id = "", user_id = "";
		boolean isExist = false;
		System.out.println("Get user role mapping");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		for (Object[] row : returnList) {
			role_id = (String) row[0];
			user_id = (String) row[1];
			System.out.println("get user role mapping: role '" + role_id
					+ "' for user " + user_id);
			if (role_id.equals(roleId) && user_id.equals(user_id)) {
				System.out.println("Exist role mapping with user");
				isExist = true;
				break;
			}
		}
		return isExist;
	}

	public int insertUserRoleMapping(String role_id, String user_id) {
		if (!getUserRoleMapping(role_id, user_id)) {
			Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
					"insertUserRoleMapping");
			sqlQuery.setParameter("role_id", role_id);
			sqlQuery.setParameter("user_id", user_id);
			System.out.println("Insert new role '" + role_id + "' for user "
					+ user_id);
			return sqlQuery.executeUpdate();
		} else {
			return -1;
		}
	}

	public String getNewUserId(String username) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getNewUserId");
		String user_id = "";
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		for (Object[] row : returnList) {
			user_id = (String) row[0];
			String user_name = (String) row[1];
			if (user_name.equals(username)) {
				System.out.println("Custom user id:" + user_id);
				break;
			}
		}
		return user_id;
	}

	public List<Object[]> getNewUsers() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getNewUsers");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		return returnList;
	}

	public int countNewRows() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"count_new_rows");
		List<Object> returnList = sqlQuery.list();
		if (returnList.size() > 0) {
			return ((Number) sqlQuery.uniqueResult()).intValue();
		} else {
			return 0;
		}
	}

	public int migrateUserEntity() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"migrate_user_entity");
		List<Object> returnList = sqlQuery.list();
		if (returnList.size() > 0) {
			return ((Number) sqlQuery.uniqueResult()).intValue();
		} else {
			return 0;
		}
	}

	public int checkCustomUserRole() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"check_custom_user_entity");
		List<Object> returnList = sqlQuery.list();
		if (returnList.size() > 0) {
			return ((Number) sqlQuery.uniqueResult()).intValue();
		} else {
			return 0;
		}
	}

	public int migrateCustomUserRole() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"migrate_role");
		List<Object> returnList = sqlQuery.list();
		if (returnList.size() > 0) {
			return ((Number) sqlQuery.uniqueResult()).intValue();
		} else {
			return 0;
		}
	}

	public List<Object[]> checkUserTypeNotMapping() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"user_type_not_mapping");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		return returnList;
	}

	public boolean checkExistUserType(String userType) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getUserType");
		String user_type = "";
		boolean isExist = false;
		System.out.println("Get user type");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		for (Object[] row : returnList) {
			user_type = (String) row[0];
			System.out.println("get user type: '" + user_type + "'");
			if (user_type.equals(userType)) {
				System.out.println("Exist user type");
				isExist = true;
				break;
			}
		}
		return isExist;
	}

	public boolean checkExistUserSubType(String userSubType) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getUserSubType");
		String user_sub_type = "";
		boolean isExist = false;
		System.out.println("Get user sub type");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		for (Object[] row : returnList) {
			user_sub_type = (String) row[0];
			System.out.println("get user type: '" + user_sub_type + "'");
			if (user_sub_type.equals(userSubType)) {
				System.out.println("Exist user sub type");
				isExist = true;
				break;
			}
		}
		return isExist;
	}

	public int insertCustomUserType(String userType, String uuid) {
		if (!checkExistUserType(userType)) {
			Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
					"insertToCustomUserType");
			sqlQuery.setParameter("user_type", userType);
			sqlQuery.setParameter("uuid", uuid);
			return sqlQuery.executeUpdate();
		} else {
			return -1;
		}
	}

	public int insertCustomUserSubType(String userSubType, String ID,
			String userTypeId) {
		if (!checkExistUserSubType(userSubType)) {
			Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
					"insertToCustomUserSubType");
			sqlQuery.setParameter("user_sub_type", userSubType);
			sqlQuery.setParameter("custom_user_type_id", userTypeId);
			sqlQuery.setParameter("id", ID);
			return sqlQuery.executeUpdate();
		} else {
			return -1;
		}
	}

	public List<Object[]> checkUserSubTypeNotMapping() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"user_sub_type_not_mapping");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		return returnList;
	}

	public String getUserTypeId(String userType) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getUserTypeId");
		sqlQuery.setParameter("user_type", userType);
		List<Object> returnList = sqlQuery.list();
		if (returnList.size() > 0) {
			return (String) sqlQuery.uniqueResult();
		} else {
			return "";
		}
	}

	public String getProperties(String key) {
		String value = "";
		try {
			Properties prop = new Properties();
			String propFileName = "frequency.properties";
			InputStream inputStream = SynDBImpl.class.getClassLoader()
					.getResourceAsStream(propFileName);

			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '"
						+ propFileName + "' not found in the classpath");
			}
			// get the property value and print it out
			value = prop.getProperty(key);
			System.out.println(key + ": " + value);
		} catch (Exception e) {
			System.out.println("readConfig: " + e);
		}

		return value;
	}
	
	public String getEnableProperties(String key) {
		String value = "";
		try {
			Properties prop = new Properties();
			String propFileName = "configuration.properties";
			InputStream inputStream = SynDBImpl.class.getClassLoader()
					.getResourceAsStream(propFileName);

			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '"
						+ propFileName + "' not found in the classpath");
			}
			// get the property value and print it out
			value = prop.getProperty(key);
			System.out.println(key + ": " + value);
		} catch (Exception e) {
			System.out.println("readConfig: " + e);
		}

		return value;
	}

	public List<Object[]> getNewRolesFromStg() {
		// getNewRolesFromStg
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getNewRolesFromStg");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		return returnList;
	}

	public List<Object[]> getPSERealmId() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getPSERealmId");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		return returnList;
	}

	public List<Object[]> getListAppsBelongRealm() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getListAppsBelongRealm");
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		return returnList;
	}

	public int insertKeycloakRole(String id, String app_realm_constraint,
			boolean application_role, String name, String realm_id,
			String application) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"insertKeycloakRole");
		sqlQuery.setParameter("id", id);
		sqlQuery.setParameter("app_realm_constraint", app_realm_constraint);
		sqlQuery.setParameter("application_role", application_role);
		sqlQuery.setParameter("name", name);
		sqlQuery.setParameter("realm_id", realm_id);
		sqlQuery.setParameter("application", application);
		return sqlQuery.executeUpdate();
	}

	public int insertToCustomUser() {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"insertToCustomUser");
		return sqlQuery.executeUpdate();
	}
}