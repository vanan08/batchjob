package com.mkyong.service.impl;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import com.mkyong.service.SynODSDB;

@Transactional
public class SynODSDBImpl implements SynODSDB, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Autowired
	@Qualifier("odsSessionFactory")
	public SessionFactory sessionFactory;

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	
	public List< Object[] > getCustomStgUsers(String createdDate) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getCustomStgUsers");
		sqlQuery.setString("created_date", createdDate);
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		System.out.println("ODS Staging getCustomStgUsers list size : " + returnList.size());
		return returnList;
	}
	
	
	public List< Object[] > getCustomStgUserRoles(String createdDate) {
		Query sqlQuery = sessionFactory.getCurrentSession().getNamedQuery(
				"getCustomStgUserRoles");
		sqlQuery.setString("created_date", createdDate);
		List<Object[]> returnList = (List<Object[]>) sqlQuery.list();
		System.out.println("ODS Staging getCustomStgUserRoles list size : " + returnList.size());
		return returnList;
	}
	
	
}