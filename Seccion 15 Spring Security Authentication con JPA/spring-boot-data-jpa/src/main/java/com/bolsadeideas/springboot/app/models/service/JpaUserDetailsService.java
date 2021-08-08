package com.bolsadeideas.springboot.app.models.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bolsadeideas.springboot.app.models.dao.IUsuarioDao;
import com.bolsadeideas.springboot.app.models.entity.Role;
import com.bolsadeideas.springboot.app.models.entity.Usuario;

@Service("jpaUserDetailsService")
public class JpaUserDetailsService implements UserDetailsService{

	@Autowired
	private IUsuarioDao usuarioDao;
	private Logger logger = LoggerFactory.getLogger(JpaUserDetailsService.class);
	
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Usuario usuario = usuarioDao.findByUsername(username); 
		
		if (usuario == null) {
			logger.error("Error login : NO existe el Usuario '".concat(username).concat("' digitado"));
			throw new UsernameNotFoundException("Usuario '".concat(username).concat("' no existe..."));
		}
		
		List<GrantedAuthority> autorities = new ArrayList<GrantedAuthority>();
		
		for (Role rol : usuario.getRoles()) {
			logger.info("Rol: ".concat(rol.getAuthority()));
			autorities.add(new SimpleGrantedAuthority(rol.getAuthority()));
		}
		
		if (autorities.isEmpty()) {
			logger.error("Error login : Usuario '".concat(username).concat("' no tiene roles asignados..."));
			throw new UsernameNotFoundException("Usuario '".concat(username).concat("' no tiene roles asignados..."));
		}
		
		return new User(usuario.getUsername(), usuario.getPassword(), usuario.getEnabled(), true, true, true, autorities);
	}

}
