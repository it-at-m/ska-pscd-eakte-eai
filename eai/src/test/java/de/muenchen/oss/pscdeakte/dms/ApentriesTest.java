package de.muenchen.oss.pscdeakte.dms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.muenchen.oss.eakte.v1.generated.model.DmsObjektResponse;
import de.muenchen.oss.eakte.v1.generated.model.Objektreferenz;
import de.muenchen.oss.eakte.v1.generated.model.ReadApentryAntwortDTO;
import de.muenchen.oss.eakte.v1.generated.model.SearchApentryResponseDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApentriesTest {

    private static final String AKTENPLANNUMMER = "9512";

    @Mock
    private DmsApentryService dmsService;

    @Test
    void initMapDoesNotCallDmsWhenInitialFillIsDisabled() {
        final Apentries apentries = new Apentries(dmsService, properties(false));

        apentries.initMap();

        verifyNoInteractions(dmsService);
    }

    @Test
    void initMapMarksMapAsInitializedWhenDmsReturnsNoResponse() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        when(dmsService.getApentries()).thenReturn(null);

        apentries.initMap();
        apentries.initMap();

        verify(dmsService).getApentries();
    }

    @Test
    void initMapHandlesResponseWithoutObjects() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO response = mock(ReadApentryAntwortDTO.class);
        when(dmsService.getApentries()).thenReturn(response);
        when(response.getGiobjecttype()).thenReturn(null);

        apentries.initMap();

        verify(response).getGiobjecttype();
    }

    @Test
    void initMapCachesMatchingReferencesAndIgnoresInvalidNames() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO response = mock(ReadApentryAntwortDTO.class);
        final Objektreferenz nullName = referenceWithName(null);
        final Objektreferenz emptyName = referenceWithName("");
        final Objektreferenz nonMatchingName = referenceWithName("9512.1/invalid");
        final Objektreferenz matchingName = referenceWithNameAndAddress("9512.1/1000005001-1000010000", "coo-cached");
        when(dmsService.getApentries()).thenReturn(response);
        when(response.getGiobjecttype()).thenReturn(List.of(nullName, emptyName, nonMatchingName, matchingName));

        apentries.initMap();

        assertEquals("coo-cached", apentries.getApentryCoo("1000005001"));
        verify(dmsService, never()).createSubjectAreaUnit(1, "1000005001-1000010000");
    }

    @Test
    void refreshMapReloadsAndReplacesExistingEntries() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO first = mock(ReadApentryAntwortDTO.class);
        final ReadApentryAntwortDTO second = mock(ReadApentryAntwortDTO.class);
        final Objektreferenz oldRef = referenceWithNameAndAddress("9512.1/1000005001-1000010000", "old-coo");
        final Objektreferenz newRef = referenceWithNameAndAddress("9512.1/1000005001-1000010000", "new-coo");
        when(first.getGiobjecttype()).thenReturn(List.of(oldRef));
        when(second.getGiobjecttype()).thenReturn(List.of(newRef));
        when(dmsService.getApentries()).thenReturn(first, second);

        apentries.initMap();
        assertEquals("old-coo", apentries.getApentryCoo("1000005001"));
        apentries.refreshMap();
        apentries.initMap();

        assertEquals("new-coo", apentries.getApentryCoo("1000005001"));
        verify(dmsService, times(2)).getApentries();
        verify(dmsService, never()).createSubjectAreaUnit(1, "1000005001-1000010000");
    }

    @Test
    void refreshMapAfterCreationTimeoutFindsPreviouslyCreatedApentry() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO response = mock(ReadApentryAntwortDTO.class);
        final Objektreferenz reference = referenceWithNameAndAddress("9512.0/1000000001-1000005000", "coo-created");
        when(dmsService.createSubjectAreaUnit(0, "1000000001-1000005000")).thenThrow(new IllegalStateException("timeout"));
        when(dmsService.getApentries()).thenReturn(response);
        when(response.getGiobjecttype()).thenReturn(List.of(reference));

        assertThrows(IllegalStateException.class, () -> apentries.getApentryCoo("1000000001"));
        apentries.refreshMap();

        assertEquals("coo-created", apentries.getApentryCoo("1000000001"));
        verify(dmsService).createSubjectAreaUnit(0, "1000000001-1000005000");
    }

    @Test
    void refreshMapUsesTheCheckedObjectList() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO response = mock(ReadApentryAntwortDTO.class);
        final Objektreferenz reference = referenceWithNameAndAddress("9512.0/1000000001-1000005000", "coo-existing");
        when(dmsService.getApentries()).thenReturn(response);
        when(response.getGiobjecttype()).thenReturn(List.of(reference)).thenReturn(null);

        apentries.refreshMap();

        assertEquals("coo-existing", apentries.getApentryCoo("1000000001"));
        verify(response).getGiobjecttype();
        verify(dmsService, never()).createSubjectAreaUnit(0, "1000000001-1000005000");
    }

    @Test
    void refreshMapKeepsExistingEntriesWhenReloadFails() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO response = mock(ReadApentryAntwortDTO.class);
        final Objektreferenz reference = referenceWithNameAndAddress("9512.1/1000005001-1000010000", "old-coo");
        when(response.getGiobjecttype()).thenReturn(List.of(reference));
        when(dmsService.getApentries()).thenReturn(response).thenThrow(new IllegalStateException("timeout"));

        apentries.initMap();
        assertThrows(IllegalStateException.class, apentries::refreshMap);

        assertEquals("old-coo", apentries.getApentryCoo("1000005001"));
    }

    @Test
    void refreshMapRejectsMissingResponseWithoutDiscardingCache() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO response = mock(ReadApentryAntwortDTO.class);
        final Objektreferenz reference = referenceWithNameAndAddress("9512.1/1000005001-1000010000", "old-coo");
        when(response.getGiobjecttype()).thenReturn(List.of(reference));
        when(dmsService.getApentries()).thenReturn(response).thenReturn(null);

        apentries.initMap();
        assertThrows(IllegalStateException.class, apentries::refreshMap);

        assertEquals("old-coo", apentries.getApentryCoo("1000005001"));
    }

    @Test
    void refreshMapRejectsMissingObjectListWithoutDiscardingCache() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO first = mock(ReadApentryAntwortDTO.class);
        final ReadApentryAntwortDTO second = mock(ReadApentryAntwortDTO.class);
        final Objektreferenz reference = referenceWithNameAndAddress("9512.1/1000005001-1000010000", "old-coo");
        when(first.getGiobjecttype()).thenReturn(List.of(reference));
        when(second.getGiobjecttype()).thenReturn(null);
        when(dmsService.getApentries()).thenReturn(first, second);

        apentries.initMap();
        assertThrows(IllegalStateException.class, apentries::refreshMap);

        assertEquals("old-coo", apentries.getApentryCoo("1000005001"));
    }

    @Test
    void refreshMapRemovesEntriesMissingFromNewResponse() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final ReadApentryAntwortDTO first = mock(ReadApentryAntwortDTO.class);
        final ReadApentryAntwortDTO second = mock(ReadApentryAntwortDTO.class);
        final Objektreferenz reference = referenceWithNameAndAddress("9512.0/1000000001-1000005000", "old-coo");
        when(first.getGiobjecttype()).thenReturn(List.of(reference));
        when(second.getGiobjecttype()).thenReturn(List.of());
        when(dmsService.getApentries()).thenReturn(first, second);
        stubCreatedApentry();

        apentries.initMap();
        apentries.refreshMap();

        assertEquals("coo-created", apentries.getApentryCoo("1000000001"));
        verify(dmsService).createSubjectAreaUnit(0, "1000000001-1000005000");
    }

    @Test
    void refreshMapDoesNotCallDmsWhenInitialFillIsDisabled() {
        final Apentries apentries = new Apentries(dmsService, properties(false));

        apentries.refreshMap();

        verifyNoInteractions(dmsService);
    }

    @Test
    void getApentryCooCreatesAndCachesMissingApentryDuringInitialFill() {
        final Apentries apentries = new Apentries(dmsService, properties(true));
        final DmsObjektResponse response = mock(DmsObjektResponse.class);
        when(dmsService.createSubjectAreaUnit(0, "1000000001-1000005000")).thenReturn(response);
        when(response.getObjid()).thenReturn("coo-created");

        assertEquals("coo-created", apentries.getApentryCoo("1000000001"));
        assertEquals("coo-created", apentries.getApentryCoo("1000000001"));

        verify(dmsService).createSubjectAreaUnit(0, "1000000001-1000005000");
    }

    @Test
    void getApentryCooReturnsExistingApentryWithoutInitialFill() {
        final Apentries apentries = new Apentries(dmsService, properties(false));
        final SearchApentryResponseDTO response = mock(SearchApentryResponseDTO.class);
        final Objektreferenz reference = referenceWithAddress("coo-existing");
        when(dmsService.getApentryFor(0)).thenReturn(response);
        when(response.getGiobjecttype()).thenReturn(List.of(reference));

        assertEquals("coo-existing", apentries.getApentryCoo("1000000001"));
    }

    @Test
    void getApentryCooCreatesApentryWhenSearchReturnsNoObjects() {
        final Apentries apentries = new Apentries(dmsService, properties(false));
        final SearchApentryResponseDTO response = mock(SearchApentryResponseDTO.class);
        when(dmsService.getApentryFor(0)).thenReturn(response);
        when(response.getGiobjecttype()).thenReturn(null);

        stubCreatedApentry();

        assertEquals("coo-created", apentries.getApentryCoo("1000000001"));
    }

    @Test
    void getApentryCooCreatesApentryWhenSearchReturnsEmptyObjects() {
        final Apentries apentries = new Apentries(dmsService, properties(false));
        final SearchApentryResponseDTO response = mock(SearchApentryResponseDTO.class);
        when(dmsService.getApentryFor(0)).thenReturn(response);
        when(response.getGiobjecttype()).thenReturn(List.of());

        stubCreatedApentry();

        assertEquals("coo-created", apentries.getApentryCoo("1000000001"));
    }

    private DmsProperties properties(final boolean initialFill) {
        final DmsProperties properties = new DmsProperties();
        properties.setAktenplannummer(AKTENPLANNUMMER);
        properties.setInitialbefuellung(initialFill);
        return properties;
    }

    private static Objektreferenz referenceWithName(final String objname) {
        final Objektreferenz reference = mock(Objektreferenz.class);
        when(reference.getObjname()).thenReturn(objname);
        return reference;
    }

    private static Objektreferenz referenceWithNameAndAddress(final String objname, final String objaddress) {
        final Objektreferenz reference = referenceWithName(objname);
        when(reference.getObjaddress()).thenReturn(objaddress);
        return reference;
    }

    private static Objektreferenz referenceWithAddress(final String objaddress) {
        final Objektreferenz reference = mock(Objektreferenz.class);
        when(reference.getObjaddress()).thenReturn(objaddress);
        return reference;
    }

    private void stubCreatedApentry() {
        final DmsObjektResponse response = mock(DmsObjektResponse.class);
        when(dmsService.createSubjectAreaUnit(0, "1000000001-1000005000")).thenReturn(response);
        when(response.getObjid()).thenReturn("coo-created");
    }

}
