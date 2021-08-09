package com.bolsadeideas.springboot.app.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bolsadeideas.springboot.app.models.entity.Cliente;
import com.bolsadeideas.springboot.app.models.service.IClienteService;
import com.bolsadeideas.springboot.app.models.service.IUploadFileService;
import com.bolsadeideas.springboot.app.util.paginator.PageRender;

@Controller
@SessionAttributes("cliente")
public class ClienteController {
	
	protected final Log logger = LogFactory.getLog(this.getClass());

	@Autowired
	private IClienteService clienteService;

	@Autowired
	private IUploadFileService uploadFileService;

	@GetMapping(value = "/uploads/{filename:.+}")
	public ResponseEntity<Resource> verFoto(@PathVariable String filename) {

		Resource recurso = null;

		try {
			recurso = uploadFileService.load(filename);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"")
				.body(recurso);
	}

	@GetMapping(value = "/ver/{id}")
	public String ver(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {

		Cliente cliente = clienteService.fetchByIdWithFacturas(id);
		if (cliente == null) {
			flash.addFlashAttribute("error", "El cliente no existe en la base de datos");
			return "redirect:/listar";
		}

		model.put("cliente", cliente);
		model.put("titulo", "Detalle cliente: " + cliente.getNombre());
		return "ver";
	}

	@RequestMapping(value = {"/listar","/"}, method = RequestMethod.GET)
	public String listar(@RequestParam(name = "page", defaultValue = "0") int page, Model model,
			Authentication authentication, HttpServletRequest request) {
		if (authentication != null) {
			logger.info("Usurio captura con Authentication ".concat(authentication.getName()));
		}
		
		Authentication userAuth = SecurityContextHolder.getContext().getAuthentication();

		if (userAuth != null) {
			logger.info("Usurio captura con SecurityContextHolder.getContext().getAuthentication() ".concat(userAuth.getName()));
		}
		
		if (hasRole("ROLE_ADMIN")) {
			logger.info("Bienvenido señor(a) ".concat(userAuth.getName()).concat(", si tiene acceso... captura de forma programatica"));
		}else {
			logger.info("Bienvenido señor(a) ".concat(userAuth.getName()).concat(", Noooooo tiene acceso... captura de forma programatica"));
			
		}
		
		//Verificar roles USANDO PREFIJO SI LO DEJAMOS VACIO lo debemos dejar en el if
		SecurityContextHolderAwareRequestWrapper securityContext = new SecurityContextHolderAwareRequestWrapper(request, "ROLE_");
		
		if(securityContext.isUserInRole("ADMIN")) {
			logger.info("Este man SIIII tiene el ROLE_ADMIN Capturado con SecurityContextHolderAwareRequestWrapper ");
			
		}else {
			logger.info("Este man NOOOOO tiene el ROLE_ADMIN Capturado con SecurityContextHolderAwareRequestWrapper");
		}
		
		
		if(request.isUserInRole("ROLE_ADMIN")) {
			logger.info("Este man SIIII tiene el ROLE_ADMIN Capturado con HttpServletRequest request ");
			
		}else {
			logger.info("Este man NOOOOO tiene el ROLE_ADMIN  Capturado con HttpServletRequest request");
		}
		Pageable pageRequest = PageRequest.of(page, 4);

		Page<Cliente> clientes = clienteService.findAll(pageRequest);

		PageRender<Cliente> pageRender = new PageRender<Cliente>("/listar", clientes);
		model.addAttribute("titulo", "Listado de clientes");
		model.addAttribute("clientes", clientes);
		model.addAttribute("page", pageRender);
		return "listar";
	}

	@RequestMapping(value = "/form")
	public String crear(Map<String, Object> model) {

		Cliente cliente = new Cliente();
		model.put("cliente", cliente);
		model.put("titulo", "Formulario de Cliente");
		return "form";
	}

	@RequestMapping(value = "/form/{id}")
	public String editar(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {

		Cliente cliente = null;

		if (id > 0) {
			cliente = clienteService.findOne(id);
			if (cliente == null) {
				flash.addFlashAttribute("error", "El ID del cliente no existe en la BBDD!");
				return "redirect:/listar";
			}
		} else {
			flash.addFlashAttribute("error", "El ID del cliente no puede ser cero!");
			return "redirect:/listar";
		}
		model.put("cliente", cliente);
		model.put("titulo", "Editar Cliente");
		return "form";
	}

	@RequestMapping(value = "/form", method = RequestMethod.POST)
	public String guardar(@Valid Cliente cliente, BindingResult result, Model model,
			@RequestParam("file") MultipartFile foto, RedirectAttributes flash, SessionStatus status) {

		if (result.hasErrors()) {
			model.addAttribute("titulo", "Formulario de Cliente");
			return "form";
		}

		if (!foto.isEmpty()) {

			if (cliente.getId() != null && cliente.getId() > 0 && cliente.getFoto() != null
					&& cliente.getFoto().length() > 0) {

				uploadFileService.delete(cliente.getFoto());
			}

			String uniqueFilename = null;
			try {
				uniqueFilename = uploadFileService.copy(foto);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			flash.addFlashAttribute("info", "Has subido correctamente '" + uniqueFilename + "'");

			cliente.setFoto(uniqueFilename);
		}

		String mensajeFlash = (cliente.getId() != null) ? "Cliente editado con éxito!" : "Cliente creado con éxito!";

		clienteService.save(cliente);
		status.setComplete();
		flash.addFlashAttribute("success", mensajeFlash);
		return "redirect:listar";
	}

	@RequestMapping(value = "/eliminar/{id}")
	public String eliminar(@PathVariable(value = "id") Long id, RedirectAttributes flash) {

		if (id > 0) {
			Cliente cliente = clienteService.findOne(id);

			clienteService.delete(id);
			flash.addFlashAttribute("success", "Cliente eliminado con éxito!");

			if (uploadFileService.delete(cliente.getFoto())) {
				flash.addFlashAttribute("info", "Foto " + cliente.getFoto() + " eliminada con exito!");
			}

		}
		return "redirect:/listar";
	}
	
	
	
	private boolean hasRole(String rol) {
		
		SecurityContext context = SecurityContextHolder.getContext();
		
		if(context == null) {
			return false;
		}
		
		
		Authentication userAuth = context.getAuthentication();
		if (userAuth == null) {
			return false;
		}
		
		//Cualquier clase ROLE o que represente un Role en nuestra aplicacion tiene que implementar esta interface GrantedAuthority
		
		/*
		Collection<? extends GrantedAuthority> authorities = userAuth.getAuthorities();
		for (GrantedAuthority authority : authorities) {
			if (rol.equals(authority.getAuthority())) {
				logger.info("Rol autenticado = ".concat(authority.getAuthority()));
				return true;
			}
		}
		
		return false;
		*/
		
		//Esta es la forma optmizada para hacer esta validacion 
		Collection<? extends GrantedAuthority> authorities = userAuth.getAuthorities();
		return authorities.contains(new SimpleGrantedAuthority(rol));
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}