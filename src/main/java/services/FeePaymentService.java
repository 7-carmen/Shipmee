package services;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.paypal.base.rest.PayPalRESTException;
import com.paypal.exception.ClientActionRequiredException;
import com.paypal.exception.HttpErrorException;
import com.paypal.exception.InvalidCredentialException;
import com.paypal.exception.InvalidResponseDataException;
import com.paypal.exception.MissingCredentialException;
import com.paypal.exception.SSLConfigurationException;
import com.paypal.sdk.exceptions.OAuthException;

import domain.FeePayment;
import domain.PayPalObject;
import domain.RouteOffer;
import domain.ShipmentOffer;
import domain.User;
import repositories.FeePaymentRepository;

@Service
@Transactional
public class FeePaymentService {
	
	static Logger log = Logger.getLogger(FeePaymentService.class);


	// Managed repository -----------------------------------------------------

	@Autowired
	private FeePaymentRepository feePaymentRepository;

	// Supporting services ----------------------------------------------------
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ActorService actorService;
	
	@Autowired
	private RouteOfferService routeOfferService;
	
	@Autowired
	private ShipmentOfferService shipmentOfferService;
	
	@Autowired
	private PayPalService payPalService;
	
	// Constructors -----------------------------------------------------------

	public FeePaymentService() {
		super();
	}

	// Simple CRUD methods ----------------------------------------------------

	public FeePayment create() {
		Assert.isTrue(actorService.checkAuthority("USER"),
				"Only an user can create a feepayment");
		
		FeePayment result;
		User user;
		
		result = new FeePayment();
		user = userService.findByPrincipal();
		
		result.setPurchaser(user);
		result.setPaymentMoment(new Date());
		result.setType("Pending");
		
		return result;
	}
	
	public FeePayment save(FeePayment feePayment) {
		Assert.notNull(feePayment);
		Assert.isTrue(actorService.checkAuthority("USER"),
				"Only an user can save a feepayment");
		
		User user;
		FeePayment feePaymentPreSave;
		
		user = userService.findByPrincipal();
		
		if(feePayment.getId() == 0) {
			feePayment.setPurchaser(user);
			feePayment.setPaymentMoment(new Date());
			feePayment.setType("Pending");
			
			feePayment = feePaymentRepository.save(feePayment);
		} else {
			feePaymentPreSave = this.findOne(feePayment.getId());
			feePaymentPreSave.setType(feePayment.getType());
			
			feePayment = feePaymentRepository.save(feePaymentPreSave);
		}	
			
		return feePayment;
	}
	
	public FeePayment manageFeePayment(int feepaymentId, String type) {
		FeePayment feePayment;
		FeePayment res = null;
		PayPalObject payPal;

		feePayment = this.findOne(feepaymentId);
		feePayment.setType(type);

		try {
			payPal = payPalService.findByFeePaymentId(feepaymentId);
			if (payPal != null) {
				payPalService.payToShipper(payPal.getTrackingId());
			}

			res = this.save(feePayment);
		} catch (SSLConfigurationException | InvalidCredentialException | HttpErrorException
				| InvalidResponseDataException | ClientActionRequiredException | MissingCredentialException
				| OAuthException | PayPalRESTException | IOException | InterruptedException e) {
			log.error(e,e);
		}

		return res;
	}

	
	public FeePayment findOne(int feePaymentId) {
		FeePayment result;
		
		result = feePaymentRepository.findOne(feePaymentId);
		
		return result;
	}
	
	public Collection<FeePayment> findAll() {
		Collection<FeePayment> result;

		result = feePaymentRepository.findAll();

		return result;
	}

	public Page<FeePayment> findAllPendingByUser(Pageable pageable) {
		Page<FeePayment> result;
		User user;
		
		user = userService.findByPrincipal();

		result = feePaymentRepository.findAllPendingByUser(user.getId(), pageable);
		Assert.notNull(result);
		
		return result;
	}
	

	// Other business methods -------------------------------------------------
	
	public Page<FeePayment> findAllRejected(Pageable page) {
		Page<FeePayment> result;

		result = feePaymentRepository.findAllRejected(page);
		Assert.notNull(result);
		return result;
	}
	
	public Page<FeePayment> findAllPending(Pageable page) {
		Page<FeePayment> result;

		result = feePaymentRepository.findAllPending(page);
		Assert.notNull(result);
		return result;
	}
	
	public Page<FeePayment> findAllAccepted(Pageable page) {
		Page<FeePayment> result;

		result = feePaymentRepository.findAllAccepted(page);
		Assert.notNull(result);
		return result;
	}
	
	public FeePayment constructFromRouteOffer(int routeOfferId){
		FeePayment res;
		RouteOffer routeOffer;
		
		res = this.create();

		routeOffer = routeOfferService.findOne(routeOfferId);
		
		res.setRouteOffer(routeOffer);
		res.setAmount(routeOffer.getAmount());
		res.setCarrier(routeOffer.getRoute().getCreator());
		
		return res;
	}
	
	public FeePayment constructFromShipmentOffer(int shipmentOfferId){
		FeePayment res;
		ShipmentOffer shipmentOffer;
		
		res = this.create();
		
		shipmentOffer = shipmentOfferService.findOne(shipmentOfferId);
		
		res.setShipmentOffer(shipmentOffer);
		res.setAmount(shipmentOffer.getAmount());
		res.setCarrier(shipmentOffer.getUser());
		
		return res;
	}
}
