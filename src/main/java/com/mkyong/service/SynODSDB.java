package com.mkyong.service;

import java.util.List;

public interface SynODSDB {
	public List< Object[] > getCustomStgUsers(String createdDate);
	public List< Object[] > getCustomStgUserRoles(String createdDate);
}
