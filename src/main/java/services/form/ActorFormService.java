package services.form;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import domain.Actor;
import domain.User;
import domain.form.ActorForm;
import security.Authority;
import security.UserAccount;
import security.UserAccountService;
import services.ActorService;
import services.UserService;

@Service
@Transactional
public class ActorFormService {

	// Supporting services ----------------------------------------------------
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ActorService actorService;
	
	@Autowired
	private UserAccountService userAccountService;
	
	@Autowired
	private Validator validator;
	
	// Constructors -----------------------------------------------------------

	public ActorFormService() {
		super();
	}

	// Simple CRUD methods ----------------------------------------------------

	public ActorForm createForm(Boolean isCreate) {
		ActorForm result;
				
		if(actorService.checkLogin() && !isCreate){
			result = this.createFromActor(actorService.findByPrincipal());
		}else{
			result = new ActorForm();
		}
		
		return result;
	}
	
	private ActorForm createFromActor(Actor a){
		ActorForm res;
		
		res = new ActorForm();
		res.setName(a.getName());
		res.setSurname(a.getSurname());
		res.setEmail(a.getEmail());
		res.setBirthDate(a.getBirthDate());
		res.setPhone(a.getPhone());
		res.setDni(a.getDni());
		res.setPhoto(a.getPhoto());
		res.setUserName(a.getUserAccount().getUsername());
		res.setId(a.getId());
		
		if(actorService.checkAuthority(Authority.USER)){
			res.setDniPhoto(userService.findByPrincipal().getDniPhoto());
		}
		
		return res;
	}
	
	public Object reconstruct(ActorForm actorForm, BindingResult binding) {
		Actor userWithUserName;
		Actor actActor;
		
		userWithUserName = actorService.findByUsername(actorForm.getUserName());

		
		// Chequear nombre de usuario �nico
		
		if (!actorService.checkLogin() ||
				(actorForm.getId() == 0 && actorService.checkAuthority(Authority.ADMIN))){
			// Registry
			
			this.addBinding(binding, actorForm.getPassword().equals(actorForm.getRepeatedPassword()),
					"repeatedPassword", "user.passwordMismatch", null);
			this.addBinding(binding, actorForm.getPassword().length() > 5,
					"password", "javax.validation.constraints.Min.message", null);
			this.addBinding(binding, userWithUserName == null, "userName", "user.userName.inUse", null);
			
			if (!actorService.checkLogin()){
				// Registry User
				User res;
				UserAccount uAccount;
				
				res = userService.create();
					// Commons
				uAccount = res.getUserAccount();
				res.setName(actorForm.getName());
				res.setSurname(actorForm.getSurname());
				res.setEmail(actorForm.getEmail());
				res.setBirthDate(actorForm.getBirthDate());
				uAccount.setUsername(actorForm.getUserName());
				uAccount.setPassword(actorForm.getPassword());
				
				uAccount = userAccountService.encodePassword(uAccount);
				res.setUserAccount(uAccount);
					// User
				this.addBinding(binding, actorForm.getAcceptLegalCondition(),
						"acceptLegalCondition", "user.rejectedLegalConditions", null);

				
				validator.validate(actorForm, binding);
				
				return res;
			}else{
				Assert.notNull(null, "Registro de usuarios (de cualquier rol) como admin no implementado");
				return null;
			}
			
		}else{
			// Editing 
			
			actActor = actorService.findByPrincipal();
			
			if(!actorForm.getPassword().equals("") || !actorForm.getRepeatedPassword().equals("")){
				// Password modified
				this.addBinding(binding, actorForm.getPassword().equals(actorForm.getRepeatedPassword()),
						"repeatedPassword", "user.passwordMismatch", null);
				this.addBinding(binding, actorForm.getPassword().length() > 5,
						"password", "javax.validation.constraints.Min.message", null);
			}
			
			if(!actorForm.getUserName().equals(actActor.getUserAccount().getUsername())){
				// UserName modified
				this.addBinding(binding, userWithUserName == null, "userName", "user.userName.inUse", null);
			}
			
			if (actorService.checkAuthority(Authority.USER)){
				// Registry User
				User res;
				UserAccount uAccount;
				
				res = userService.findOne(actActor.getId());
					// Commons
				uAccount = res.getUserAccount();
				res.setName(actorForm.getName());
				res.setSurname(actorForm.getSurname());
				res.setEmail(actorForm.getEmail());
				res.setBirthDate(actorForm.getBirthDate());
				res.setPhone(actorForm.getPhone());
				res.setDni(actorForm.getDni());
				res.setDniPhoto(actorForm.getDniPhoto());

				res.setPhoto(actorForm.getPhoto());
				
				uAccount.setUsername(actorForm.getUserName());
				
				if(!actorForm.getPassword().equals("") || !actorForm.getRepeatedPassword().equals("")){
					uAccount.setPassword(actorForm.getPassword());
				
					uAccount = userAccountService.encodePassword(uAccount);
				}
				res.setUserAccount(uAccount);
				
				validator.validate(actorForm, binding);
				
				return res;
			}else{
				Assert.notNull(null, "Edici�n de usuarios no Authority.USER no implementado");
				return null;
			}
		}

	}
	
	private void addBinding(Errors errors, boolean mustBeTrue, String field, String validationError, Object[] other){
		if (!mustBeTrue){
			errors.rejectValue(field, validationError, other, "");
		}
	}
}