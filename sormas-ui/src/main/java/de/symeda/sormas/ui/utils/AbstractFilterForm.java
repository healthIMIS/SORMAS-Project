package de.symeda.sormas.ui.utils;

import static de.symeda.sormas.ui.utils.LayoutUtil.div;
import static de.symeda.sormas.ui.utils.LayoutUtil.filterLocs;
import static de.symeda.sormas.ui.utils.LayoutUtil.loc;

import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;

import com.vaadin.event.ShortcutAction;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.fieldgroup.FieldGroup;
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.ui.AbstractTextField;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.PopupDateField;
import com.vaadin.v7.ui.TextField;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.region.DistrictReferenceDto;
import de.symeda.sormas.api.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.ui.UserProvider;

public abstract class AbstractFilterForm<T> extends AbstractForm<T> {

	private static final long serialVersionUID = -692949260096914243L;

	public static final String FILTER_ITEM_STYLE = "filter-item";

	private static final String RESET_BUTTON_ID = "reset";
	protected static final String APPLY_BUTTON_ID = "apply";
	private static final String EXPAND_COLLAPSE_ID = "expandCollapse";
	private static final String MORE_FILTERS_ID = "moreFilters";

	private CustomLayout moreFiltersLayout;
	private boolean skipChangeEvents;
	private boolean hasFilter;

	protected AbstractFilterForm(Class<T> type, String propertyI18nPrefix) {

		super(type, propertyI18nPrefix, new SormasFieldGroupFieldFactory(null, null), true);

		String moreFiltersHtmlLayout = createMoreFiltersHtmlLayout();
		boolean hasMoreFilters = moreFiltersHtmlLayout != null && moreFiltersHtmlLayout.length() > 0;

		if (hasMoreFilters) {
			moreFiltersLayout = new CustomLayout();
			moreFiltersLayout.setTemplateContents(moreFiltersHtmlLayout);
			moreFiltersLayout.setVisible(false);
			getContent().addComponent(moreFiltersLayout, MORE_FILTERS_ID);

			addMoreFilters(moreFiltersLayout);
		}

		addResetHandler(e -> onButtonClick());
		addApplyHandler(e -> onButtonClick());
		this.addValueChangeListener(e -> onFilterChange());

		addDefaultButtons();
	}

	// hide buttons when no filters remain
	public void onButtonClick() {
		hasFilter = streamFieldsForEmptyCheck(getContent()).anyMatch(f -> !f.isEmpty());
		if (!hasFilter) {
			getContent().getComponent(RESET_BUTTON_ID).setVisible(false);
			Component applyButton = getContent().getComponent(APPLY_BUTTON_ID);
			if (applyButton != null) {
				applyButton.setVisible(false);
			}
		}
	}

	// show buttons when there are filters
	public void onFilterChange() {
		hasFilter = streamFieldsForEmptyCheck(getContent()).anyMatch(f -> !f.isEmpty());
		if (hasFilter) {
			getContent().getComponent(RESET_BUTTON_ID).setVisible(true);
			Component applyButton = getContent().getComponent(APPLY_BUTTON_ID);
			if (applyButton != null) {
				applyButton.setVisible(true);
			}
		}
	}

	@Override
	protected String createHtmlLayout() {
		return div(
			filterLocs(ArrayUtils.addAll(getMainFilterLocators(), EXPAND_COLLAPSE_ID, RESET_BUTTON_ID, APPLY_BUTTON_ID)) + loc(MORE_FILTERS_ID));
	}

	protected abstract String[] getMainFilterLocators();

	protected String createMoreFiltersHtmlLayout() {
		return "";
	}

	public void addMoreFilters(CustomLayout moreFiltersContainer) {

	}

	protected void addDefaultButtons() {

		Button resetButton = ButtonHelper.createButton(Captions.actionResetFilters, null, FILTER_ITEM_STYLE);
		getContent().addComponent(resetButton, RESET_BUTTON_ID);
		resetButton.setVisible(false);

		Button applyButton = ButtonHelper.createButton(Captions.actionApplyFilters, null, FILTER_ITEM_STYLE);
		applyButton.setClickShortcut(ShortcutAction.KeyCode.ENTER);
		getContent().addComponent(applyButton, APPLY_BUTTON_ID);
		applyButton.setVisible(false);

		if (moreFiltersLayout != null) {
			String showMoreCaption = I18nProperties.getCaption(Captions.actionShowMoreFilters);
			Button showHideMoreButton =
				ButtonHelper.createIconButtonWithCaption("showHideMoreFilters", showMoreCaption, VaadinIcons.CHEVRON_DOWN, e -> {
					Button showHideButton = e.getButton();
					boolean isShowMore = showHideButton.getCaption().equals(showMoreCaption);
					showHideButton.setCaption(isShowMore ? I18nProperties.getCaption(Captions.actionShowLessFilters) : showMoreCaption);
					showHideButton.setIcon(isShowMore ? VaadinIcons.CHEVRON_UP : VaadinIcons.CHEVRON_DOWN);

					if (isShowMore) {
						moreFiltersLayout.setVisible(true);
					} else {
						moreFiltersLayout.setVisible(false);
					}
				}, ValoTheme.BUTTON_BORDERLESS, CssStyles.VSPACE_TOP_NONE, CssStyles.LABEL_PRIMARY, RESET_BUTTON_ID);

			getContent().addComponent(showHideMoreButton, EXPAND_COLLAPSE_ID);
		}
	}

	protected CustomLayout getMoreFiltersContainer() {
		return moreFiltersLayout;
	}

	@Override
	@SuppressWarnings({
		"rawtypes",
		"unchecked" })
	protected <T1 extends Field> void formatField(T1 field, String propertyId) {

		super.formatField(field, propertyId);

		field.addStyleName(FILTER_ITEM_STYLE);

		String caption = I18nProperties.getPrefixCaption(propertyI18nPrefix, propertyId, field.getCaption());
		setFieldCaption(field, caption);

		if (TextField.class.isAssignableFrom(field.getClass())) {
			((TextField) field).addTextChangeListener(e -> field.setValue(e.getText()));
		}

		field.addValueChangeListener(e -> {
			onFieldValueChange(propertyId, e);
		});
	}

	public void addResetHandler(Button.ClickListener resetHandler) {
		Button resetButton = (Button) getContent().getComponent(RESET_BUTTON_ID);
		if (resetButton != null) {
			resetButton.addClickListener(resetHandler);
		}
	}

	public void addApplyHandler(Button.ClickListener applyHandler) {
		Button applyButton = (Button) getContent().getComponent(APPLY_BUTTON_ID);
		if (applyButton != null) {
			applyButton.addClickListener(applyHandler);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected void applyFieldConfiguration(FieldConfiguration configuration, Field field) {
		super.applyFieldConfiguration(configuration, field);

		if (configuration.getCaption() != null) {
			setFieldCaption(field, configuration.getCaption());
		}
	}

	@Override
	public void setValue(T newFieldValue) throws ReadOnlyException, Converter.ConversionException {

		doWithoutChangeHandler(() -> {
			super.setValue(newFieldValue);

			applyDependenciesOnNewValue(newFieldValue);

			if (moreFiltersLayout != null) {
				boolean hasExpandedFilter = streamFieldsForEmptyCheck(moreFiltersLayout).anyMatch(f -> !f.isEmpty());
				moreFiltersLayout.setVisible(hasExpandedFilter);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	protected Stream<Field> streamFieldsForEmptyCheck(CustomLayout layout) {
		return FieldHelper.streamFields(layout);
	}

	protected void applyDependenciesOnNewValue(T newValue) {

	}

	protected void applyRegionFilterDependency(RegionReferenceDto region, String districtFieldId) {
		UserDto user = UserProvider.getCurrent().getUser();
		ComboBox districtField = getField(districtFieldId);
		if (user.getRegion() != null && user.getDistrict() == null) {
			districtField.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(user.getRegion().getUuid()));
			districtField.setEnabled(true);
		} else {
			if (region != null) {
				districtField.addItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(region.getUuid()));
				districtField.setEnabled(true);
			} else {
				districtField.setEnabled(false);
			}
		}
	}

	protected void applyRegionAndDistrictFilterDependency(
		RegionReferenceDto region,
		String districtFieldId,
		DistrictReferenceDto district,
		String communityFieldId) {
		applyRegionFilterDependency(region, districtFieldId);
		applyDistrictDependency(district, communityFieldId);
	}

	protected void applyDistrictDependency(DistrictReferenceDto district, String communityFieldId) {
		UserDto user = UserProvider.getCurrent().getUser();
		ComboBox communityField = getField(communityFieldId);
		if (user.getDistrict() != null && user.getCommunity() == null) {
			communityField.addItems(FacadeProvider.getCommunityFacade().getAllActiveByDistrict(user.getDistrict().getUuid()));
			communityField.setEnabled(true);
		} else {
			if (district != null) {
				communityField.addItems(FacadeProvider.getCommunityFacade().getAllActiveByDistrict(district.getUuid()));
				communityField.setEnabled(true);
			} else {
				communityField.setEnabled(false);
			}
		}
	}

	private void onFieldValueChange(String propertyId, Property.ValueChangeEvent event) {
		if (!skipChangeEvents) {
			try {
				doWithoutChangeHandler(() -> applyDependenciesOnFieldChange(propertyId, event));

				this.getFieldGroup().commit();
				this.fireValueChange(true);
			} catch (FieldGroup.CommitException ex) {
				ex.printStackTrace();
			}
		}
	}

	protected void applyDependenciesOnFieldChange(String propertyId, Property.ValueChangeEvent event) {
	}

	private void doWithoutChangeHandler(Callable callback) {
		this.skipChangeEvents = true;
		try {
			callback.call();
		} finally {
			this.skipChangeEvents = false;
		}
	}

	@SuppressWarnings("rawtypes")
	private <T1 extends Field> void setFieldCaption(T1 field, String caption) {
		field.setCaption(null);
		if (field instanceof ComboBox) {
			((ComboBox) field).setInputPrompt(caption);
		} else if (field instanceof AbstractTextField) {
			((AbstractTextField) field).setInputPrompt(caption);
		} else if (field instanceof PopupDateField) {
			((PopupDateField) field).setInputPrompt(caption);
		} else {
			field.setCaption(caption);
		}
	}

	public boolean hasFilter() {
		return hasFilter;
	}

	protected void clearFields(String... propertyIds) {
		for (String propertyId : propertyIds) {
			getField(propertyId).setValue(null);
		}
	}

	protected void clearAndDisableFields(String... propertyIds) {
		for (String propertyId : propertyIds) {
			Field<?> field = getField(propertyId);

			field.setValue(null);
			field.setEnabled(false);
		}
	}

	protected void enableFields(String... propertyIds) {
		for (String propertyId : propertyIds) {
			getField(propertyId).setEnabled(true);
		}
	}

	interface Callable {

		void call();
	}
}
