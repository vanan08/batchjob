package com.mkyong.service;

import java.util.List;

public interface SynDB{

	public void query();
	
	public int countNewRows();
	
	public int migrateUserEntity();
	
	public int checkExistStgUser(String username);
	public int checkExistStgUserRole(String username, String roleName);
	
	public int checkCustomUserRole();
	public int migrateCustomUserRole();
	
	public List< Object[] > checkUserTypeNotMapping();
	public List< Object[] > checkUserSubTypeNotMapping();
	public int insertCustomUserType(String userType, String ID);
	public int insertCustomUserSubType(String userSubType, String ID, String userTypeId);
	
	public String getUserTypeId(String userType);
	public String getUserSubTypeId(String userSubType);
	public List< Object[] > getNewRolesFromStg();
	public List< Object[] > getPSERealmId();
	public List< Object[] > getListAppsBelongRealm();
	
	public int insertKeycloakRole(String id, String app_realm_constraint, boolean application_role, String name, 
    		String realm_id, String application);
	
	public List< Object[] > getNewUsers();
	public String getNewUserId(String username);
	public int insertUserRoleMapping(String role_id, String user_id);
	public String getRoleId(String rolename);
	
	public String getProperties(String key);
	
	public boolean getUserRoleMapping(String roleId, String userId);
	
	public boolean checkExistUserType(String userType);
	public boolean checkExistUserSubType(String userSubType);
	
	public List< String > getUserTypeRoles(String key);
	public List< Object[] > getUserRealm();
	
	public int insertToCustomUser();
	
	public String getMaxCreatedDateSTGUser();
	public String getMaxCreatedDateStgUserRoles();
	
	public int insertSTGUser(String id_nric, String first_name,
			String last_name, String email, String account_status,
			String mobile, String agent_code, String agency, String need2fa,
			String needtnc, String user_type, String user__sub_type);
	
	public int clearUserTypeSubType();
	public int deleteSubType();
	public int deleteUserType();
	public int insertSTGUserRole(String id_nric, String role_name);
	public String getConfigProperties(String key);
	public List< Object[] > getListUserTypesUserNames();
	public int updateUserTypeSubTypeForUserEntity(String custom_user_type_id, String custom_user_subtype_id, String username);
	public int checkExistUserEntity(String username);
	public int insertToUserEntity(String ID_NRIC, String FIRST_NAME, String LAST_NAME, String MOBILE, String EMAIL, String ACCOUNT_STATUS, String AGENT_CODE, String AGENCY, String NEED2FA, String NEEDTNC);
}