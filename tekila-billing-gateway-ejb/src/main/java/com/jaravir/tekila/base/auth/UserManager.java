package com.jaravir.tekila.base.auth;

import com.jaravir.tekila.base.auth.persistence.Group;
import com.jaravir.tekila.base.auth.persistence.User;

import java.io.Serializable;
import java.security.MessageDigest;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
//import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import static spring.security.Constants.USER_GROUP;

//@Singleton
//@Startup
@Stateless
public class UserManager  implements Serializable {
	@PersistenceContext(unitName = "tekila")
	private EntityManager em;
	@Resource
	private EJBContext ctx;

	private transient final static Logger log = Logger.getLogger(UserManager.class);

//	@Resource
//    private SessionContext sessionContext;
        
	//@PostConstruct
	@RolesAllowed("administrators")
	public void createUser() {				
		
		String password = "callccenter";
		Group gr = new Group("callcenter");
		User usr = null;
		
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(password.getBytes("UTF-8"));
			//usr = new User("cc", Base64.encodeBase64String(md.digest()), gr);	
			em.persist(usr);
		}
		catch (Exception ex) {
			//log.debug(ex.toString());
			ctx.setRollbackOnly();
			log.error("Error while encoding password: ", ex);
		}
	}
	
	public EntityManager getEntityManager() {
		return this.em;
	}
	
	public User findByUsername (String username) {
		return (User) this.getEntityManager()		
		.createQuery("select u from User u left join fetch u.group group left join fetch group.roles where u.userName = :username")
		.setParameter("username", username)
		.getSingleResult();
	}

//	public void addUserGroupToSessionContext(Group group){
//		sessionContext.getContextData().put(USER_GROUP, group);
//	}
}
