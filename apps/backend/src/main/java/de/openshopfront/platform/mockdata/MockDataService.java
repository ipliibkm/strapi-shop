package de.openshopfront.platform.mockdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.openshopfront.platform.auth.StrapiAdminClient;
import de.openshopfront.platform.booking.BookingOfferingService;
import de.openshopfront.platform.commerce.CatalogService;
import de.openshopfront.platform.page.PageService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills the whole application with one of five test store profiles: business profile, product
 * catalog, booking services and published storefront pages. Intended for testing, not production.
 */
@Service
public class MockDataService {
  private static final String BUSINESS_PROFILE_PATH =
      "/content-manager/single-types/api::business-profile.business-profile";
  private static final ObjectMapper BLOCKS = new ObjectMapper();

  /** One test product with a single variant and its main image address. */
  public record ProductSpec(
      String slug, String name, String description, long priceCents, int stock, String imageUrl) {}

  /** One bookable service with its duration in minutes. */
  public record ServiceSpec(String name, int durationMinutes) {}

  /** The complete test data of one store. */
  public record StoreProfile(
      String key,
      String businessName,
      String profileSlug,
      String contactEmail,
      List<ProductSpec> products,
      List<ServiceSpec> services,
      Map<String, List<JsonNode>> pages) {}

  /** What one apply run changed. */
  public record Summary(int products, int services, int pages) {}

  private final CatalogService catalog;
  private final BookingOfferingService offerings;
  private final PageService pages;
  private final StrapiAdminClient strapi;
  private final ObjectMapper json;

  public MockDataService(
      CatalogService catalog,
      BookingOfferingService offerings,
      PageService pages,
      StrapiAdminClient strapi,
      ObjectMapper json) {
    this.catalog = catalog;
    this.offerings = offerings;
    this.pages = pages;
    this.strapi = strapi;
    this.json = json;
  }

  /** The supported store keys, in display order. */
  public static List<String> profileKeys() {
    return STORES.keySet().stream().toList();
  }

  /** Replaces the application content with one test store. Rolls back on any failure. */
  @Transactional
  public Summary apply(String key, HttpSession session) {
    StoreProfile store = STORES.get(key);
    if (store == null) throw new IllegalArgumentException("Unknown test store: " + key);
    updateBusinessProfile(store, session);
    int products = replaceProducts(store);
    int services = replaceServices(store);
    int pageCount = savePages(store, session);
    return new Summary(products, services, pageCount);
  }

  private void updateBusinessProfile(StoreProfile store, HttpSession session) {
    JsonNode current = strapi.content(session, "GET", BUSINESS_PROFILE_PATH, null);
    ObjectNode body = json.createObjectNode();
    if (current.has("data")) body.setAll((ObjectNode) current.get("data"));
    body.put("name", store.businessName());
    body.put("slug", store.profileSlug());
    body.put("contactEmail", store.contactEmail());
    strapi.content(session, "PUT", BUSINESS_PROFILE_PATH, body);
  }

  private int replaceProducts(StoreProfile store) {
    for (CatalogService.Product product : catalog.list(false))
      catalog.delete(product.id().toString());
    for (ProductSpec spec : store.products()) {
      catalog.create(
          spec.slug(),
          spec.name(),
          spec.description(),
          spec.imageUrl().isBlank() ? List.of() : List.of(spec.imageUrl()),
          new CatalogService.VariantInput(
              spec.slug() + "-std", "Standard", spec.priceCents(), spec.stock()));
    }
    return store.products().size();
  }

  private int replaceServices(StoreProfile store) {
    // Hard delete: the name has a unique constraint, so deactivated rows would block re-apply.
    for (BookingOfferingService.Offering offering : offerings.list())
      offerings.delete(offering.id());
    for (ServiceSpec spec : store.services()) offerings.create(spec.name(), spec.durationMinutes());
    return store.services().size();
  }

  private int savePages(StoreProfile store, HttpSession session) {
    int count = 0;
    for (Map.Entry<String, List<JsonNode>> entry : store.pages().entrySet()) {
      pages.save(entry.getKey(), titleFor(entry.getKey()), true, entry.getValue(), session);
      pages.publish(entry.getKey(), session);
      count++;
    }
    return count;
  }

  private static String titleFor(String slug) {
    return switch (slug) {
      case "home" -> "Home";
      case "services" -> "Services";
      case "booking" -> "Book a Visit";
      case "shop" -> "Shop";
      case "about" -> "About Us";
      case "contact" -> "Contact";
      default -> slug;
    };
  }

  // ---------- block builders ----------

  private static JsonNode heading(String eyebrow, String title) {
    ObjectNode block = BLOCKS.createObjectNode();
    return block.put("type", "heading").put("eyebrow", eyebrow).put("title", title);
  }

  private static JsonNode text(String html) {
    ObjectNode block = BLOCKS.createObjectNode();
    return block.put("type", "text").put("html", "<p>" + html + "</p>");
  }

  /** A gallery block of themed stock photos, shown on the store's home page. */
  private static JsonNode gallery(String eyebrow, String title, String... captions) {
    ObjectNode block = BLOCKS.createObjectNode();
    block.put("type", "gallery").put("eyebrow", eyebrow).put("title", title);
    ArrayNode items = block.putArray("items");
    for (String caption : captions) {
      items.add(BLOCKS.createObjectNode().put("url", unsplash(caption)).put("caption", caption));
    }
    return block;
  }

  /** A stable stock photo address for one theme word. */
  private static String unsplash(String theme) {
    return "https://picsum.photos/seed/%s/800/600".formatted(theme.toLowerCase().replace(' ', '-'));
  }

  private static List<JsonNode> blocks(JsonNode... items) {
    return new ArrayList<>(List.of(items));
  }

  // ---------- the five test stores ----------

  private static final Map<String, StoreProfile> STORES = buildStores();

  private static Map<String, StoreProfile> buildStores() {
    Map<String, StoreProfile> stores = new LinkedHashMap<>();

    stores.put(
        "shoes",
        new StoreProfile(
            "shoes",
            "Stride & Sole",
            "stride-and-sole",
            "hello@strideandsole.example",
            List.of(
                new ProductSpec(
                    "runner-pro",
                    "Runner Pro",
                    "Lightweight daily trainer with responsive cushioning.",
                    12900,
                    24,
                    unsplash("running-shoes")),
                new ProductSpec(
                    "trail-blazer",
                    "Trail Blazer",
                    "Grip-focused boot for rough terrain and wet weather.",
                    14500,
                    18,
                    unsplash("hiking-boots")),
                new ProductSpec(
                    "court-classic",
                    "Court Classic",
                    "Timeless low-top with full-grain leather upper.",
                    8900,
                    32,
                    unsplash("leather-sneakers")),
                new ProductSpec(
                    "urban-slip-on",
                    "Urban Slip-On",
                    "Breathable knit slip-on for the city.",
                    7500,
                    40,
                    unsplash("knit-shoes")),
                new ProductSpec(
                    "winter-boot",
                    "Winter Boot",
                    "Insulated boot rated for deep cold.",
                    16000,
                    12,
                    unsplash("winter-boots")),
                new ProductSpec(
                    "kids-sneaker",
                    "Kids Sneaker",
                    "Durable sneaker that grows with the foot.",
                    5500,
                    50,
                    unsplash("kids-shoes"))),
            List.of(
                new ServiceSpec("Fitting consultation", 30),
                new ServiceSpec("Sole repair", 45),
                new ServiceSpec("Shoe care workshop", 60)),
            Map.of(
                "home",
                    blocks(
                        heading("FOOTWEAR STUDIO", "Step into something new."),
                        text(
                            "Quality footwear, honest advice and repairs that last. Find the pair that fits your life."),
                        gallery(
                            "OUR WORK",
                            "From the bench",
                            "Running shoes",
                            "Leather boots",
                            "Sneakers",
                            "Winter footwear")),
                "services",
                    blocks(
                        heading("OUR SERVICES", "Fit, care and repair"),
                        text(
                            "Book a fitting consultation, send in worn soles or join a shoe care workshop.")),
                "booking",
                    blocks(
                        heading("BOOK A VISIT", "Reserve your time with us"),
                        text(
                            "Choose a service and a time that works for you. We confirm instantly.")),
                "shop",
                    blocks(
                        heading("SHOP", "Our collection."),
                        text("Every pair is available in full sizes. Shipping within the EU.")),
                "about",
                    blocks(
                        heading("OUR STORY", "Built around good shoes"),
                        text(
                            "We started as a repair bench and grew into a studio where fitting, care and sales meet.")),
                "contact",
                    blocks(
                        heading("CONTACT", "Find your pair."),
                        text(
                            "Write to us or stop by the studio. We answer within one business day.")))));

    stores.put(
        "flowers",
        new StoreProfile(
            "flowers",
            "Bloom & Petal",
            "bloom-and-petal",
            "hello@bloompetal.example",
            List.of(
                new ProductSpec(
                    "seasonal-bouquet",
                    "Seasonal Bouquet",
                    "A hand-tied bouquet of whatever is at its best this week.",
                    3900,
                    20,
                    unsplash("flower-bouquet")),
                new ProductSpec(
                    "rose-arrangement",
                    "Rose Arrangement",
                    "Twelve long-stem roses in a wrapped box.",
                    5500,
                    15,
                    unsplash("red-roses")),
                new ProductSpec(
                    "dried-flower-set",
                    "Dried Flower Set",
                    "Preserved stems that keep their shape for years.",
                    4500,
                    25,
                    unsplash("dried-flowers")),
                new ProductSpec(
                    "potted-orchid",
                    "Potted Orchid",
                    "A healthy potted orchid with care guide.",
                    2900,
                    30,
                    unsplash("orchid-flower")),
                new ProductSpec(
                    "wedding-centerpiece",
                    "Wedding Centerpiece",
                    "A table centerpiece designed for your venue.",
                    18000,
                    6,
                    unsplash("wedding-flowers")),
                new ProductSpec(
                    "plant-care-kit",
                    "Plant Care Kit",
                    "Everything a new plant owner needs in one box.",
                    1900,
                    45,
                    unsplash("houseplants"))),
            List.of(
                new ServiceSpec("Bouquet consultation", 30),
                new ServiceSpec("Event floristry session", 90),
                new ServiceSpec("Subscription setup", 30)),
            Map.of(
                "home",
                    blocks(
                        heading("FLOWER STUDIO", "Fresh stems, honest craft"),
                        text(
                            "Seasonal flowers cut at their peak, arranged by hand. For everyday moments and the big ones."),
                        gallery(
                            "OUR WORK",
                            "This week's arrangements",
                            "Bouquets",
                            "Roses",
                            "Dried flowers",
                            "Wedding florals")),
                "services",
                    blocks(
                        heading("OUR SERVICES", "From bouquet to ballroom"),
                        text(
                            "Consult on a bouquet, plan event floristry or set up a weekly subscription.")),
                "booking",
                    blocks(
                        heading("BOOK A VISIT", "Reserve your time with us"),
                        text(
                            "Choose a service and a time that works for you. We confirm instantly.")),
                "shop",
                    blocks(
                        heading("SHOP", "This week's flowers."),
                        text(
                            "Availability changes daily with the market. Order early for same-day pickup.")),
                "about",
                    blocks(
                        heading("OUR STORY", "A studio that smells like morning"),
                        text(
                            "We buy from regional growers, cut in the cool of the day and arrange everything by hand.")),
                "contact",
                    blocks(
                        heading("CONTACT", "Tell us about your occasion."),
                        text("Dates, colors, budgets — write to us and we will make it bloom.")))));

    stores.put(
        "tattoo",
        new StoreProfile(
            "tattoo",
            "Ink & Iron",
            "ink-and-iron",
            "studio@inkandiron.example",
            List.of(
                new ProductSpec(
                    "tattoo-balm",
                    "Tattoo Balm",
                    "Fragrance-free balm for fresh and healing ink.",
                    1500,
                    60,
                    unsplash("skincare-balm")),
                new ProductSpec(
                    "aftercare-set",
                    "Aftercare Set",
                    "Balm, wash and patch kit in one box.",
                    2900,
                    40,
                    unsplash("gift-box")),
                new ProductSpec(
                    "flash-sheet-print",
                    "Flash Sheet Print",
                    "A signed print of a flash design from the wall.",
                    1000,
                    25,
                    unsplash("art-print")),
                new ProductSpec(
                    "studio-hoodie",
                    "Studio Hoodie",
                    "Heavyweight hoodie with the studio mark.",
                    4500,
                    30,
                    unsplash("hoodie")),
                new ProductSpec(
                    "ink-sampler-kit",
                    "Ink Sampler Kit",
                    "A small kit to try our house inks at home.",
                    1900,
                    20,
                    unsplash("ink-bottles"))),
            List.of(
                new ServiceSpec("Consultation", 30),
                new ServiceSpec("Small piece session", 60),
                new ServiceSpec("Large piece session", 180)),
            Map.of(
                "home",
                    blocks(
                        heading("TATTOO STUDIO", "Ink that earns its keep"),
                        text(
                            "Fine line, blackwork and traditional. One artist at a time, no rush, aftercare included."),
                        gallery(
                            "OUR WORK",
                            "Recent pieces",
                            "Fine line tattoo",
                            "Blackwork tattoo",
                            "Traditional tattoo",
                            "Flash design")),
                "services",
                    blocks(
                        heading("OUR SERVICES", "From idea to healed skin"),
                        text(
                            "Book a consultation first, then reserve the session your piece needs.")),
                "booking",
                    blocks(
                        heading("BOOK A VISIT", "Reserve your time with us"),
                        text(
                            "Choose a service and a time that works for you. We confirm instantly.")),
                "shop",
                    blocks(
                        heading("SHOP", "Studio goods."),
                        text("Aftercare, prints and wearables from the studio shelf.")),
                "about",
                    blocks(
                        heading("OUR STORY", "A small bench with high standards"),
                        text(
                            "We keep the studio small on purpose: one chair, strict hygiene, and time for your idea.")),
                "contact",
                    blocks(
                        heading("CONTACT", "Send us your idea."),
                        text(
                            "Describe the piece you want — size, placement, references. We reply within two days.")))));

    stores.put(
        "bakery",
        new StoreProfile(
            "bakery",
            "Golden Crust",
            "golden-crust",
            "hello@goldencrust.example",
            List.of(
                new ProductSpec(
                    "sourdough-loaf",
                    "Sourdough Loaf",
                    "Naturally leavened loaf with a deep crust.",
                    650,
                    40,
                    unsplash("sourdough-bread")),
                new ProductSpec(
                    "croissant-box",
                    "Croissant Box",
                    "Six butter croissants baked that morning.",
                    1200,
                    30,
                    unsplash("croissants")),
                new ProductSpec(
                    "cinnamon-roll",
                    "Cinnamon Roll",
                    "Soft roll with cardamom glaze.",
                    450,
                    60,
                    unsplash("cinnamon-rolls")),
                new ProductSpec(
                    "celebration-cake",
                    "Celebration Cake",
                    "A layered cake made to order for your date.",
                    8500,
                    8,
                    unsplash("birthday-cake")),
                new ProductSpec(
                    "rye-bread",
                    "Rye Bread",
                    "Dense dark rye with caraway.",
                    700,
                    35,
                    unsplash("dark-rye-bread")),
                new ProductSpec(
                    "pastry-gift-set",
                    "Pastry Gift Set",
                    "A box of assorted pastries for two.",
                    3500,
                    20,
                    unsplash("pastries"))),
            List.of(
                new ServiceSpec("Cake consultation", 30),
                new ServiceSpec("Baking workshop", 90),
                new ServiceSpec("Private tasting", 60)),
            Map.of(
                "home",
                    blocks(
                        heading("BAKERY", "Baked before the sun is up"),
                        text(
                            "Sourdough, rye and laminated doughs made in small batches every morning."),
                        gallery(
                            "OUR WORK",
                            "Fresh from the oven",
                            "Sourdough bread",
                            "Croissants",
                            "Cinnamon rolls",
                            "Celebration cake")),
                "services",
                    blocks(
                        heading("OUR SERVICES", "More than a counter"),
                        text("Plan a cake, learn to bake or taste through our weekly lineup.")),
                "booking",
                    blocks(
                        heading("BOOK A VISIT", "Reserve your time with us"),
                        text(
                            "Choose a service and a time that works for you. We confirm instantly.")),
                "shop",
                    blocks(
                        heading("SHOP", "Today's bakes."),
                        text("Quantities are small by design. Order the day before for pickup.")),
                "about",
                    blocks(
                        heading("OUR STORY", "Flour, water, salt and time"),
                        text(
                            "We bake with a 12-year-old starter and no shortcuts. The crust is the point.")),
                "contact",
                    blocks(
                        heading("CONTACT", "Tell us about your celebration."),
                        text("Dates, flavors, allergies — write to us and we will shape it.")))));

    stores.put(
        "fashion",
        new StoreProfile(
            "fashion",
            "Atelier Nord",
            "atelier-nord",
            "hello@ateliernord.example",
            List.of(
                new ProductSpec(
                    "wool-overcoat",
                    "Wool Overcoat",
                    "A structured overcoat in heavy merino wool.",
                    24000,
                    10,
                    unsplash("wool-coat")),
                new ProductSpec(
                    "linen-shirt",
                    "Linen Shirt",
                    "Relaxed shirt in European flax linen.",
                    8500,
                    25,
                    unsplash("linen-shirt")),
                new ProductSpec(
                    "tailored-trousers",
                    "Tailored Trousers",
                    "High-rise trousers with a clean break.",
                    12000,
                    20,
                    unsplash("trousers")),
                new ProductSpec(
                    "silk-scarf",
                    "Silk Scarf",
                    "Hand-rolled silk scarf in a seasonal print.",
                    6500,
                    30,
                    unsplash("silk-scarf")),
                new ProductSpec(
                    "leather-belt",
                    "Leather Belt",
                    "Full-grain belt with a solid brass buckle.",
                    4500,
                    35,
                    unsplash("leather-belt")),
                new ProductSpec(
                    "knit-sweater",
                    "Knit Sweater",
                    "Chunky knit in undyed wool.",
                    9500,
                    18,
                    unsplash("wool-sweater"))),
            List.of(
                new ServiceSpec("Styling appointment", 45),
                new ServiceSpec("Alteration consultation", 30),
                new ServiceSpec("Wardrobe session", 90)),
            Map.of(
                "home",
                    blocks(
                        heading("FASHION ATELIER", "Fewer pieces, better fit"),
                        text(
                            "A small wardrobe of durable clothes, cut to measure and made to be kept."),
                        gallery(
                            "OUR WORK",
                            "The current collection",
                            "Wool overcoat",
                            "Linen shirt",
                            "Tailored trousers",
                            "Silk scarf")),
                "services",
                    blocks(
                        heading("OUR SERVICES", "Fit is a service"),
                        text(
                            "Book styling, alterations or a full wardrobe session with our atelier team.")),
                "booking",
                    blocks(
                        heading("BOOK A VISIT", "Reserve your time with us"),
                        text(
                            "Choose a service and a time that works for you. We confirm instantly.")),
                "shop",
                    blocks(
                        heading("SHOP", "The current collection."),
                        text("Each piece is produced in small runs. When it is gone, it is gone.")),
                "about",
                    blocks(
                        heading("OUR STORY", "Made to be kept"),
                        text(
                            "We buy fabric from mills we know and cut every pattern ourselves. Nothing leaves the atelier unfitted.")),
                "contact",
                    blocks(
                        heading("CONTACT", "Start a conversation."),
                        text(
                            "Questions about fit, fabric or a piece you are looking for — write to us.")))));

    return stores;
  }
}
