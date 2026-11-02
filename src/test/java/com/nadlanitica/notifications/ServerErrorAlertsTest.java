package com.nadlanitica.notifications;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerErrorAlertsTest {

    private final NotificationEmailService email = mock(NotificationEmailService.class);
    private final DailyAlertGate gate = mock(DailyAlertGate.class);
    private final ServerErrorAlerts alerts = new ServerErrorAlerts(email, gate);

    @Test
    void firstOccurrenceOfTheDaySendsTheDetails() {
        when(gate.firstToday(startsWith("server-error:IllegalStateException@"))).thenReturn(true);

        alerts.report("POST /api/equity/calculate", new IllegalStateException("boom"));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(email).sendAlert(subject.capture(), body.capture());
        assertThat(subject.getValue()).contains("IllegalStateException");
        assertThat(body.getValue()).contains("POST /api/equity/calculate").contains("boom").contains("ServerErrorAlertsTest");
    }

    @Test
    void repeatsOnTheSameDayStayQuiet() {
        when(gate.firstToday(anyString())).thenReturn(false);

        alerts.report("GET /api/x", new IllegalStateException("boom"));

        verify(email, never()).sendAlert(anyString(), anyString());
    }
}
