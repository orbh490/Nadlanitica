package com.nadlanitica.mortgage;

import com.nadlanitica.common.AppState;
import com.nadlanitica.common.AppStateRepository;
import com.nadlanitica.notifications.NotificationEmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoiRateServiceTest {

    private final NotificationEmailService email = mock(NotificationEmailService.class);
    private final AppStateRepository appState = mock(AppStateRepository.class);
    private final BoiRateService service = new BoiRateService(email, appState);

    @Test
    void sendsAlertWhenRateChangesFromTheStoredValue() {
        when(appState.findById("boi.base-rate-percent")).thenReturn(Optional.of(new AppState("boi.base-rate-percent", "3.25")));

        service.onRateFetched(3.5);

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(email).sendAlert(subject.capture(), anyString());
        assertThat(subject.getValue()).contains("עלתה").contains("3.25").contains("3.50");
        ArgumentCaptor<AppState> saved = ArgumentCaptor.forClass(AppState.class);
        verify(appState).save(saved.capture());
        assertThat(saved.getValue().getStateValue()).isEqualTo("3.5");
        assertThat(service.currentBaseRatePercent()).isEqualTo(3.5);
    }

    @Test
    void noAlertWhenRateIsUnchanged() {
        when(appState.findById("boi.base-rate-percent")).thenReturn(Optional.of(new AppState("boi.base-rate-percent", "3.25")));

        service.onRateFetched(3.25);

        verify(email, never()).sendAlert(anyString(), anyString());
        verify(appState, never()).save(any());
    }

    @Test
    void firstFetchOnlyStoresTheRate() {
        when(appState.findById("boi.base-rate-percent")).thenReturn(Optional.empty());

        service.onRateFetched(3.25);

        verify(email, never()).sendAlert(anyString(), anyString());
        verify(appState).save(any());
    }
}
