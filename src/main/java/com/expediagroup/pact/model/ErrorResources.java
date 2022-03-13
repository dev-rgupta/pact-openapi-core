package com.expediagroup.pact.model;

import com.expediagroup.pact.utilities.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResources {

    private String name;
    private String level;
    private String recommendation;
    private String timeStamp;
    private String correlationId;
    private String message;
    private String description;
    private String documentationLink;
    private Collection<OffendingInputs> offendingInputs = new ArrayList<>();
    private Collection<ErrorPropertyBag> additionalPropertyBags = new ArrayList<>();

    /**
     * Encode the given Long in base 62
     *
     * @param n Number to encode
     * @return Long encoded as base 62
     */
    private static String encodeBase62(long n) {
        final String base62Chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder builder = new StringBuilder();
        long index = Long.remainderUnsigned(n, 62);
        builder.append(base62Chars.charAt((int) index));
        n = Long.divideUnsigned(n, 62);
        // now the long is unsigned, can just do regular math ops
        while (n > 0) {
            builder.append(base62Chars.charAt((int) (n % 62)));
            n /= 62;
        }
        return builder.toString();
    }

    public ErrorResources getCompleteDescription(int errorCode) {
        HttpStatus httpStatus = HttpStatus.valueOf(errorCode);
        String reasonPhrase = httpStatus.getReasonPhrase().toLowerCase().replace(" ", "-");
        HttpStatus.Series series = HttpStatus.Series.valueOf(errorCode);
        offendingInputs.add(new OffendingInputs());
        additionalPropertyBags.add(new ErrorPropertyBag());

        ErrorResources errorResourceNotFound = ErrorResources.builder()
                .name("urn:expedia-platform-error-type:" + reasonPhrase).level("Error").recommendation(series.name())
                .timeStamp(new LocalDateTime().toString()).message(reasonPhrase).description(reasonPhrase)
                .documentationLink("https://docs.api.expedia.com/errors/urn:expedia-platform-error-type:" + reasonPhrase
                        + "/en-us/details.htm")
                .correlationId(UUID.randomUUID().toString())
                .additionalPropertyBags(additionalPropertyBags)
                .offendingInputs(offendingInputs).build();

        return errorResourceNotFound;
    }

    @Override
    public String toString() {
        return "ErrorResources [name=" + name + ", level=" + level + ", recommendation=" + recommendation
                + ", timeStamp=" + timeStamp + ", correlationId=" + correlationId + ", message=" + message
                + ", description=" + description + ", documentationLink=" + documentationLink + ", offendingInputs="
                + offendingInputs + ", additionalPropertyBags=" + additionalPropertyBags + "]";
    }

}
