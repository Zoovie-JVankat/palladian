package ws.palladian.retrieval.resources;

import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.persistence.json.Jsonable;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * An arbitrary instance of content from the Web (like a Web page, feed entry, link, image, etc.)
 * </p>
 *
 * @author Philipp Katz
 */
public interface WebContent extends Jsonable {
    /**
     * @return Internal identifier of this content, used in case this item is stored in a database, or <code>-1</code>,
     * in case no identifier exists or the item has not been persisted.
     */
    int getId();

    /**
     * @return The URL pointing to this content.
     */
    String getUrl();

    /**
     * @return The title of this content.
     */
    String getTitle();

    /**
     * @return A textual summary of this content.
     */
    String getSummary();

    /**
     * @return The publication date of this content.
     */
    Date getPublished();

    /**
     * @return The geographic coordinate assigned with this content.
     */
    GeoCoordinate getCoordinate();

    /**
     * @return A source-specific identifier of this content.
     */
    String getIdentifier();

    /**
     * @return A set of (usually) human-assigned tags or keyword about the content, or an empty set if no tags were
     * assigned.
     */
    Set<String> getTags();

    /**
     * @return Name of the source, from which this {@link WebContent} was acquired.
     */
    String getSource();

    /**
     * @return A map, with arbitrary additional content. An empty map, in case no additional data exists, never
     * <code>null</code>.
     */
    Map<String, Object> getAdditionalData();

    @Override
    default JsonObject asJson() {
        JsonObject json = new JsonObject();
        json.put("id", getId());
        json.put("url", getUrl());
        json.put("title", getTitle());
        json.put("summary", getSummary());
        json.put("published", getPublished());
        json.put("coordinate", getCoordinate());
        json.put("identifier", getIdentifier());
        json.put("tags", getTags());
        json.put("source", getSource());
        json.put("additionalData", getAdditionalData());
        return json;
    }
}
