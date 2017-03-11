package services;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import domain.Route;
import domain.RouteOffer;
import domain.User;
import repositories.RouteOfferRepository;

@Service
@Transactional
public class RouteOfferService {

	// Managed repository -----------------------------------------------------

	@Autowired
	private RouteOfferRepository routeOfferRepository;

	// Supporting services ----------------------------------------------------

	@Autowired
	private UserService userService;

	@Autowired
	private RouteService routeService;

	// Constructors -----------------------------------------------------------

	public RouteOfferService() {
		super();
	}

	// Simple CRUD methods ----------------------------------------------------

	public RouteOffer create(int routeId) {
		RouteOffer res;
		Route route;

		route = routeService.findOne(routeId);
		Assert.notNull(route, "service.routeOffer.create.isNullRoute");

		res = new RouteOffer();
		res.setRoute(route);
		res.setUser(userService.findByPrincipal());

		return res;
	}

	public RouteOffer createFromClone(int routeOfferId) {
		RouteOffer res;
		RouteOffer act;
		act = this.findOne(routeOfferId);
		Assert.notNull(act, "service.routeOffer.createFromClone.isNullROute");

		res = this.create(act.getRoute().getId());
		res.setAmount(act.getAmount());
		res.setDescription(act.getDescription());

		return res;
	}

	public RouteOffer save(RouteOffer input) {
		User actUser;
		RouteOffer tmp;

		Assert.notNull(input, "service.routeOffer.save.isNull");

		actUser = userService.findByPrincipal();

		if (actUser.equals(input.getUser())) { // User that create route
			if (input.getId() != 0) {
				tmp = this.findOne(input.getId());
				Assert.notNull(tmp, "service.routeOffer.save.creator.dontFindID");
				Assert.isTrue(tmp.getUser().equals(actUser), "service.routeOffer.save.creator.fake");
				Assert.isTrue(!tmp.getAcceptedByCarrier() && !tmp.getRejectedByCarrier(),
						"service.routeOffer.save.creator.isRejectedOrAccepted");
			} else {
				tmp = this.create(input.getRoute().getId());
			}

			tmp.setAmount(input.getAmount());
			tmp.setDescription(input.getDescription());
		} else if (actUser.equals(input.getRoute().getCreator())) { // User that
																	// put the
																	// offer
			Assert.isTrue(input.getId() != 0, "service.routeOffer.save.ProposerCreating"); // The
																							// routeCreator
																							// can't

			tmp = this.findOne(input.getId());
			Assert.notNull(tmp, "service.routeOffer.save.trans.dontFindID");
			Assert.isTrue(tmp.getRoute().getCreator().equals(actUser), "service.routeOffer.save.trans.fake");
			tmp.setAcceptedByCarrier(input.getAcceptedByCarrier());
			tmp.setRejectedByCarrier(input.getRejectedByCarrier());
		} else {
			Assert.isTrue(false, "service.routeOffer.save.userNotPermitted");
			return null;
		}
		Assert.isTrue(!tmp.getUser().equals(tmp.getRoute().getCreator()),
				"service.routeOffer.save.equalsCreatorAndProposer");

		tmp = routeOfferRepository.save(tmp);

		return tmp;
	}

	public void delete(int routeOfferId) {
		RouteOffer a;

		a = this.findOne(routeOfferId);

		Assert.isTrue(this.checkPermission(a), "service.routeOffer.delete.notPermitted");

		routeOfferRepository.delete(a);
	}

	public RouteOffer findOne(int routeOfferId) {
		RouteOffer result;
		result = routeOfferRepository.findOne(routeOfferId);

		if (!this.checkPermission(result)) {
			result = null;
		}

		return result;
	}

	// Other business methods -------------------------------------------------

	public void flush() {
		routeOfferRepository.flush();
	}

	public Collection<RouteOffer> findAllByRouteId(int routeId) {
		Collection<RouteOffer> result;
		User actUser;

		actUser = userService.findByPrincipal();

		result = routeOfferRepository.findAllByRouteId(routeId);

		if (!result.isEmpty()) {
			Assert.isTrue(result.iterator().next().getRoute().getCreator().equals(actUser),
					"service.routeOffer.delete.notPermitted");
		}

		return result;
	}

	// IDs could be <= 0 to ignore in the find
	public Page<RouteOffer> findAllByOrRouteIdAndOrUserId(int routeId, int userId, Pageable page) {
		Page<RouteOffer> result;
		User actUser;
		Assert.isTrue(routeId + userId > 0, "service.routeOffer.findAllByOrRouteIdAndOrUserId.notRouteOrUser");

		actUser = userService.findByPrincipal();

		result = routeOfferRepository.findAllByRouteIdAndUserId(routeId, userId, page);

		if (!result.hasContent()) {
			if (routeId > 0 && userId <= 0) {
				Assert.isTrue(result.iterator().next().getRoute().getCreator().equals(actUser),
						"service.routeOffer.findAllByOrShipmentIdAndOrUserId.notCreator");
			} else if (userId > 0 && routeId <= 0) {
				Assert.isTrue(result.iterator().next().getUser().equals(actUser),
						"service.routeOffer.findAllByOrShipmentIdAndOrUserId.notPermittedUser");
			} else if (!checkPermission(result.iterator().next())) {
				Assert.isTrue(false, "service.routeOffer.findAllByOrShipmentIdAndOrUserId.notPermitted");
			}
		}

		return result;
	}

	private boolean checkPermission(RouteOffer input) {
		User actUser;

		actUser = userService.findByPrincipal(); // Inside check if it's null

		if (input != null) {
			return actUser.equals(input.getUser()) || actUser.equals(input.getRoute().getCreator());
		} else {
			return false;
		}
	}

}
