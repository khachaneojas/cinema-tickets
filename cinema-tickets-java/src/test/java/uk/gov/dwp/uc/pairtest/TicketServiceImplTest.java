package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class TicketServiceImplTest {

    private static final Long VALID_ACCOUNT_ID = 1L;

    private TicketPaymentService paymentService;
    private SeatReservationService seatReservationService;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        seatReservationService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(
                paymentService,
                seatReservationService
        );
    }

    @Test
    void shouldPurchaseSingleAdultTicket() {
        ticketService.purchaseTickets(
                VALID_ACCOUNT_ID,
                adult(1)
        );

        verify(paymentService).makePayment(VALID_ACCOUNT_ID, 25);
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 1);

        verifyNoMoreInteractions(paymentService, seatReservationService);
    }

    @Test
    void shouldCalculatePaymentAndSeatsForMixedTicketTypes() {
        ticketService.purchaseTickets(
                VALID_ACCOUNT_ID,
                adult(2),
                child(3),
                infant(2)
        );

        verify(paymentService).makePayment(VALID_ACCOUNT_ID, 95);
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 5);

        verifyNoMoreInteractions(paymentService, seatReservationService);
    }

    @Test
    void shouldNotChargeOrReserveSeatsForInfants() {
        ticketService.purchaseTickets(
                VALID_ACCOUNT_ID,
                adult(1),
                infant(3)
        );

        verify(paymentService).makePayment(VALID_ACCOUNT_ID, 25);
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 1);

        verifyNoMoreInteractions(paymentService, seatReservationService);
    }

    @Test
    void shouldAllowMaximumOfTwentyFiveTickets() {
        ticketService.purchaseTickets(
                VALID_ACCOUNT_ID,
                adult(1),
                child(12),
                infant(12)
        );

        verify(paymentService).makePayment(VALID_ACCOUNT_ID, 205);
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 13);

        verifyNoMoreInteractions(paymentService, seatReservationService);
    }

    @Test
    void shouldAllowTwentyFiveAdultTickets() {
        ticketService.purchaseTickets(
                VALID_ACCOUNT_ID,
                adult(25)
        );

        verify(paymentService).makePayment(VALID_ACCOUNT_ID, 625);
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 25);

        verifyNoMoreInteractions(paymentService, seatReservationService);
    }

    @Test
    void shouldCombineRepeatedTicketTypes() {
        ticketService.purchaseTickets(
                VALID_ACCOUNT_ID,
                adult(1),
                adult(2),
                child(1),
                child(2)
        );

        verify(paymentService).makePayment(VALID_ACCOUNT_ID, 120);
        verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 6);

        verifyNoMoreInteractions(paymentService, seatReservationService);
    }

    @Test
    void shouldAcceptAnyPositiveAccountId() {
        long accountId = Long.MAX_VALUE;

        ticketService.purchaseTickets(accountId, adult(1));

        verify(paymentService).makePayment(accountId, 25);
        verify(seatReservationService).reserveSeat(accountId, 1);

        verifyNoMoreInteractions(paymentService, seatReservationService);
    }

    @Test
    void shouldRejectNullAccountId() {
        assertInvalidPurchase(null, adult(1));
    }

    @Test
    void shouldRejectZeroAccountId() {
        assertInvalidPurchase(0L, adult(1));
    }

    @Test
    void shouldRejectNegativeAccountId() {
        assertInvalidPurchase(-1L, adult(1));
    }

    @Test
    void shouldRejectNullTicketRequests() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                (TicketTypeRequest[]) null
        );
    }

    @Test
    void shouldRejectEmptyTicketRequests() {
        assertInvalidPurchase(VALID_ACCOUNT_ID);
    }

    @Test
    void shouldRejectNullTicketRequest() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                adult(1),
                null
        );
    }

    @Test
    void shouldRejectNullTicketType() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                new TicketTypeRequest(null, 1)
        );
    }

    @Test
    void shouldRejectZeroTicketQuantity() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                adult(1),
                child(0)
        );
    }

    @Test
    void shouldRejectNegativeTicketQuantity() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                adult(1),
                child(-1)
        );
    }

    @Test
    void shouldRejectMoreThanTwentyFiveTickets() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                adult(26)
        );
    }

    @Test
    void shouldCountInfantsTowardsPurchaseLimit() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                adult(1),
                infant(25)
        );
    }

    @Test
    void shouldRejectMoreThanTwentyFiveAcrossMultipleRequests() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                adult(10),
                child(10),
                infant(6)
        );
    }

    @Test
    void shouldRejectChildTicketsWithoutAdult() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                child(2)
        );
    }

    @Test
    void shouldRejectInfantTicketsWithoutAdult() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                infant(2)
        );
    }

    @Test
    void shouldRejectChildAndInfantTicketsWithoutAdult() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                child(2),
                infant(1)
        );
    }

    @Test
    void shouldRejectVeryLargeTicketQuantity() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                adult(Integer.MAX_VALUE)
        );
    }

    @Test
    void shouldRejectLargeCombinedQuantitiesWithoutOverflow() {
        assertInvalidPurchase(
                VALID_ACCOUNT_ID,
                adult(Integer.MAX_VALUE),
                child(Integer.MAX_VALUE)
        );
    }

    @Test
    void shouldIncludeMeaningfulErrorMessage() {
        InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(0L, adult(1))
        );

        assertEquals(
                "Account ID must be greater than zero.",
                exception.getMessage()
        );

        verifyNoInteractions(paymentService, seatReservationService);
    }

    private void assertInvalidPurchase(
            Long accountId,
            TicketTypeRequest... requests) {

        InvalidPurchaseException exception = assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(accountId, requests)
        );

        assertTrue(
                exception.getMessage() != null
                        && !exception.getMessage().isBlank(),
                "Invalid purchases should provide a meaningful error message"
        );

        verifyNoInteractions(paymentService, seatReservationService);
    }

    private TicketTypeRequest adult(int quantity) {
        return new TicketTypeRequest(TicketTypeRequest.Type.ADULT, quantity);
    }

    private TicketTypeRequest child(int quantity) {
        return new TicketTypeRequest(TicketTypeRequest.Type.CHILD, quantity);
    }

    private TicketTypeRequest infant(int quantity) {
        return new TicketTypeRequest(TicketTypeRequest.Type.INFANT, quantity);
    }
}