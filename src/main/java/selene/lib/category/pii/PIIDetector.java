package selene.lib.category.pii;

import ai.philterd.phileas.PhileasConfiguration;
import ai.philterd.phileas.model.filtering.FilterType;
import ai.philterd.phileas.model.filtering.Span;
import ai.philterd.phileas.model.filtering.TextFilterResult;

import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.filters.*;
import ai.philterd.phileas.policy.filters.Currency;
import ai.philterd.phileas.policy.filters.Date;
import ai.philterd.phileas.services.context.ContextService;
import ai.philterd.phileas.services.context.DefaultContextService;
import ai.philterd.phileas.services.disambiguation.vector.InMemoryVectorService;
import ai.philterd.phileas.services.disambiguation.vector.VectorService;
import ai.philterd.phileas.services.filters.filtering.PlainTextFilterService;
import ai.philterd.phileas.services.strategies.dynamic.*;
import ai.philterd.phileas.services.strategies.rules.*;
import lombok.SneakyThrows;


import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PIIDetector {

    private final PlainTextFilterService filterService;
    private final Policy policy;

    public PIIDetector() {
        Properties props = new Properties();
        PhileasConfiguration config = new PhileasConfiguration(props);
        ContextService ctx = new DefaultContextService();
        VectorService vtx = new InMemoryVectorService();
        this.filterService = new PlainTextFilterService(config, ctx, vtx);
        this.policy = buildPolicy();


    }

    private Policy buildPolicy() {
        Policy policy = new Policy();
        Identifiers identifiers = new Identifiers();

        addAllFilters(identifiers);
        try{
            policy.setIdentifiers(identifiers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return policy;
    }

    /**
     * Detect all PII spans in text
     */
    public List<Span> detect(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            TextFilterResult response = filterService.filter(policy, "ctx", text);
            List<Span> spans = response.getExplanation().identifiedSpans();
            return spans != null ? spans : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Detection error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Set<FilterType> getCategoryType(String text){
        List<Span> spans = detect(text);
        if (spans.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(spans.stream().map(Span::getFilterType).toList());
    }

    /**
     * Get the primary PII category for a string
     * Returns null if no PII detected
     */
    public String getCategory(String text) {
        List<Span> spans = detect(text);

        if (spans.isEmpty()) {
            return null;
        }

        // Count frequency of each category
        Map<String, Integer> frequency = new HashMap<>();
        for (Span span : spans) {
            String category = span.getFilterType().toString();
            frequency.merge(category, 1, Integer::sum);
        }

        // Return most frequent category
        return frequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Get frequency count of each PII type in text
     */
    public Map<String, Integer> getFrequency(Iterable<String> texts) {
        Map<String, Integer> frequency = new HashMap<>();

        for (String text : texts) {
            if (text == null || text.trim().isEmpty()) {
                continue;
            }

            List<Span> spans = detect(text);
            for (Span span : spans) {
                String category = span.getFilterType().toString();
                frequency.merge(category, 1, Integer::sum);
            }
        }

        return frequency;
    }

    /**
     * Overload: Single string version (keeps backward compatibility)
     */
    public Map<String, Integer> getFrequency(String text) {
        return getFrequency(Collections.singletonList(text));
    }

    /**
     * Check if text contains any PII
     */
    public boolean hasPII(String text) {
        return !detect(text).isEmpty();
    }

    /**
     * Generic helper to create a filter with REDACT strategy
     */
    private <F, S> F createFilter(
            Supplier<F> filterSupplier,
            Supplier<S> strategySupplier,
            BiConsumer<F, List<S>> strategySetter) {

        F filter = filterSupplier.get();
        S strategy = strategySupplier.get();

        // Set strategy to REDACT
        try {
            strategy.getClass().getMethod("setStrategy", String.class).invoke(strategy, "REDACT");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set strategy", e);
        }

        strategySetter.accept(filter, Collections.singletonList(strategy));
        return filter;
    }

    @SneakyThrows
    private void addAllFilters(Identifiers identifiers) {
        // Standard filters (no IOException)
        identifiers.setEmailAddress(createFilter(EmailAddress::new, EmailAddressFilterStrategy::new, EmailAddress::setEmailAddressFilterStrategies));
        identifiers.setPhoneNumber(createFilter(PhoneNumber::new, PhoneNumberFilterStrategy::new, PhoneNumber::setPhoneNumberFilterStrategies));
        identifiers.setSsn(createFilter(Ssn::new, SsnFilterStrategy::new, Ssn::setSsnFilterStrategies));
        identifiers.setCreditCard(createFilter(CreditCard::new, CreditCardFilterStrategy::new, CreditCard::setCreditCardFilterStrategies));
        identifiers.setIpAddress(createFilter(IpAddress::new, IpAddressFilterStrategy::new, IpAddress::setIpAddressFilterStrategies));
        identifiers.setDate(createFilter(Date::new, DateFilterStrategy::new, Date::setDateFilterStrategies));
        identifiers.setUrl(createFilter(Url::new, UrlFilterStrategy::new, Url::setUrlFilterStrategies));
        identifiers.setAge(createFilter(Age::new, AgeFilterStrategy::new, Age::setAgeFilterStrategies));
        identifiers.setBankRoutingNumber(createFilter(BankRoutingNumber::new, BankRoutingNumberFilterStrategy::new, BankRoutingNumber::setBankRoutingNumberFilterStrategies));
        identifiers.setBitcoinAddress(createFilter(BitcoinAddress::new, BitcoinAddressFilterStrategy::new, BitcoinAddress::setBitcoinFilterStrategies));
        identifiers.setDriversLicense(createFilter(DriversLicense::new, DriversLicenseFilterStrategy::new, DriversLicense::setDriversLicenseFilterStrategies));
        identifiers.setIbanCode(createFilter(IbanCode::new, IbanCodeFilterStrategy::new, IbanCode::setIbanCodeFilterStrategies));
        identifiers.setMacAddress(createFilter(MacAddress::new, MacAddressFilterStrategy::new, MacAddress::setMacAddressFilterStrategies));
        identifiers.setPassportNumber(createFilter(PassportNumber::new, PassportNumberFilterStrategy::new, PassportNumber::setPassportNumberFilterStrategies));
        identifiers.setVin(createFilter(Vin::new, VinFilterStrategy::new, Vin::setVinFilterStrategies));
        identifiers.setCurrency(createFilter(Currency::new, CurrencyFilterStrategy::new, Currency::setCurrencyFilterStrategies));
        identifiers.setTrackingNumber(createFilter(TrackingNumber::new, TrackingNumberFilterStrategy::new, TrackingNumber::setTrackingNumberFilterStrategies));
        identifiers.setStateAbbreviation(createFilter(StateAbbreviation::new, StateAbbreviationFilterStrategy::new, StateAbbreviation::setStateAbbreviationsFilterStrategies));

        // Dynamic filters (require dictionaries)
        identifiers.setCity(
                createDynamicFilter(City::new,
                CityFilterStrategy::new,
                City::setCityFilterStrategies));
        identifiers.setCounty(
                createDynamicFilter(County::new, CountyFilterStrategy::new, County::setCountyFilterStrategies));
        identifiers.setState(createDynamicFilter(State::new, StateFilterStrategy::new, State::setStateFilterStrategies));
        identifiers.setHospital(createDynamicFilter(Hospital::new, HospitalFilterStrategy::new, Hospital::setHospitalFilterStrategies));

        ZipCode zipCode = new ZipCode();
        ZipCodeFilterStrategy zipCodeFilterStrategy = new ZipCodeFilterStrategy();
        zipCode.setZipCodeFilterStrategies(Collections.singletonList(zipCodeFilterStrategy));
        identifiers.setZipCode(zipCode);


        identifiers.setPhoneNumberExtension(createDynamicFilter(
                PhoneNumberExtension::new,
                PhoneNumberExtensionFilterStrategy::new,
                PhoneNumberExtension::setPhoneNumberExtensionFilterStrategies
        ));
        identifiers.setStreetAddress(createDynamicFilter(StreetAddress::new, StreetAddressFilterStrategy::new, StreetAddress::setStreetAddressFilterStrategies));
        identifiers.setFirstName(createDynamicFilter(FirstName::new, FirstNameFilterStrategy::new, FirstName::setFirstNameFilterStrategies));
        identifiers.setSurname(createDynamicFilter(Surname::new, SurnameFilterStrategy::new, Surname::setSurnameFilterStrategies));
        identifiers.setPhysicianName(createDynamicFilter(PhysicianName::new, PhysicianNameFilterStrategy::new, PhysicianName::setPhysicianNameFilterStrategies));



    }

    private <F, S> F createDynamicFilter(Supplier<F> filterSupplier, Supplier<S> strategySupplier, BiConsumer<F, List<S>> strategySetter) {
        F filter = filterSupplier.get();
        S strategy = strategySupplier.get();

        try {
            strategy.getClass().getMethod("setStrategy", String.class).invoke(strategy, "REDACT");
        } catch (Exception e) {
            // Fallback to default strategy
        }

        strategySetter.accept(filter, Collections.singletonList(strategy));
        return filter;
    }


}