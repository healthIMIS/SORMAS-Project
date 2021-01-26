package de.symeda.sormas.ui.events.eventParticipantLink;

import java.util.List;

import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.event.EventParticipantCriteria;
import de.symeda.sormas.api.event.EventParticipantIndexDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.person.PersonReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.utils.PaginationList;

public class EventParticipantList extends PaginationList<EventParticipantIndexDto> {

	private final EventParticipantCriteria eventParticipantCriteria = new EventParticipantCriteria();
	private final Label noEventParticipantLabel;

	public EventParticipantList(PersonReferenceDto personRef) {
		super(5);
		eventParticipantCriteria.setPerson(personRef);
		noEventParticipantLabel = new Label(I18nProperties.getCaption(Captions.personNoEventParticipantLinkedToPerson));
	}

	@Override
	public void reload() {
		List<EventParticipantIndexDto> eventParticipants =
			FacadeProvider.getEventParticipantFacade().getIndexList(eventParticipantCriteria, 0, maxDisplayedEntries * 20, null);

		setEntries(eventParticipants);
		if (!eventParticipants.isEmpty()) {
			showPage(1);
		} else {
			listLayout.removeAllComponents();
			updatePaginationLayout();
			listLayout.addComponent(noEventParticipantLabel);
		}
	}

	@Override
	protected void drawDisplayedEntries() {
		List<EventParticipantIndexDto> displayedEntries = getDisplayedEntries();
		for (int i = 0, displayedEntriesSize = displayedEntries.size(); i < displayedEntriesSize; i++) {
			final EventParticipantIndexDto eventParticipant = displayedEntries.get(i);
			final EventParticipantListEntry listEntry = new EventParticipantListEntry(eventParticipant);
			if (UserProvider.getCurrent().hasUserRight(UserRight.EVENTPARTICIPANT_EDIT)) {
				listEntry.addEditListener(
					i,
					(Button.ClickListener) event -> ControllerProvider.getEventParticipantController()
						.navigateToData(listEntry.getEventParticipant().getUuid()));
			}

			listLayout.addComponent(listEntry);
		}
	}
}