package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Objects;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    private static final int MAX_TICKETS_PER_PURCHASE = 25;
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;

    private final TicketPaymentService paymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl() {
        this(new TicketPaymentServiceImpl(), new SeatReservationServiceImpl());
    }

    public TicketServiceImpl(
            TicketPaymentService paymentService,
            SeatReservationService seatReservationService) {

        this.paymentService = Objects.requireNonNull(
                paymentService, "Payment service must not be null");

        this.seatReservationService = Objects.requireNonNull(
                seatReservationService, "Seat reservation service must not be null");
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validatePurchase(accountId, ticketTypeRequests);

        int totalAmountToPay = 0;
        int totalSeatsToAllocate = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {

            int quantity = request.getNoOfTickets();

            switch (request.getTicketType()) {
                case ADULT -> {
                    totalAmountToPay += quantity * ADULT_TICKET_PRICE;
                    totalSeatsToAllocate += quantity;
                }
                case CHILD -> {
                    totalAmountToPay += quantity * CHILD_TICKET_PRICE;
                    totalSeatsToAllocate += quantity;
                }
                case INFANT -> {
                    // Infants are free and do not require seats.
                }
            }
        }

        paymentService.makePayment(accountId, totalAmountToPay);
        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);
    }

    private void validatePurchase(
            Long accountId,
            TicketTypeRequest... ticketTypeRequests) {

        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account ID must be greater than zero.");
        }

        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket must be requested.");
        }

        long totalTickets = 0;
        boolean hasAdult = false;

        for (TicketTypeRequest request : ticketTypeRequests) {

            if (request == null) {
                throw new InvalidPurchaseException("Ticket request cannot be null.");
            }

            if (request.getTicketType() == null) {
                throw new InvalidPurchaseException("Ticket type must be specified.");
            }

            int quantity = request.getNoOfTickets();

            if (quantity <= 0) {
                throw new InvalidPurchaseException("Ticket quantity must be greater than zero.");
            }

            totalTickets += quantity;

            if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
                throw new InvalidPurchaseException("A maximum of 25 tickets can be purchased at a time.");
            }

            if (request.getTicketType() == TicketTypeRequest.Type.ADULT) {
                hasAdult = true;
            }
        }

        if (!hasAdult) {
            throw new InvalidPurchaseException("At least one adult ticket is required.");
        }
    }

}
