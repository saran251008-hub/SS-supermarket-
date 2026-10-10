package com.sssupermarket.service;

import com.sssupermarket.dto.AiChatMessageDto;
import com.sssupermarket.dto.AiChatRequest;
import com.sssupermarket.dto.AiChatResponse;
import com.sssupermarket.entity.Product;
import com.sssupermarket.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiChatService {

    private final ProductRepository productRepository;

    public AiChatService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public AiChatResponse processMessage(AiChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return new AiChatResponse(
                    "Hello! How can I assist you today? You can ask about product ratings, availability, prices, or recommendations.",
                    List.of("Top rated items", "Fresh fruits", "Delivery info")
            );
        }

        String query = request.getMessage().trim();
        String lower = query.toLowerCase();

        // 1. Current product context if provided, or inferred from chat history
        Product cp = null;
        if (request.getProductId() != null) {
            cp = productRepository.findById(request.getProductId()).orElse(null);
        } else if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            boolean isFollowUp = lower.contains("it") || lower.contains("this") || lower.contains("that")
                    || lower.contains("how to use") || lower.contains("price") || lower.contains("stock")
                    || lower.contains("rating") || lower.contains("review") || lower.contains("explain")
                    || lower.contains("benefit") || lower.contains("detail") || lower.contains("feature")
                    || lower.contains("specs") || lower.contains("how much");
            if (isFollowUp) {
                cp = extractProductFromHistory(request.getHistory());
            }
        }

        if (cp != null) {
            if (lower.contains("this product") || lower.contains("it") || lower.contains("rating") || lower.contains("stock")
                    || lower.contains("price") || lower.contains("detail") || lower.contains("how to use") || lower.contains("usage")
                    || lower.contains("use") || lower.contains("explain") || lower.contains("about") || lower.contains("benefit")
                    || lower.contains("specs") || lower.contains("guide") || lower.contains("feature") || lower.contains("how much")) {
                return explainProduct(cp, lower.contains("how to use") || lower.contains("usage") || lower.contains("use"));
            }
        }

        // 2. Chat history summary / recap
        if (lower.contains("chat history") || lower.contains("conversation history") || lower.contains("what did we discuss")
                || lower.contains("what did we talk about") || lower.contains("previous question") || lower.contains("our chat")) {
            return summarizeChatHistory(request.getHistory());
        }

        // 3. Webpage Navigation Guide
        if (lower.contains("navigate") || lower.contains("navigation") || lower.contains("webpage") || lower.contains("website")
                || lower.contains("how to use the site") || lower.contains("how to use this site")
                || lower.contains("how to use the webpage") || lower.contains("how to use webpage")
                || lower.contains("how to use the website") || lower.contains("how to use website")
                || lower.contains("sitemap") || lower.contains("site map")
                || lower.contains("how to order") || lower.contains("how to buy")
                || lower.contains("tour") || (lower.contains("help") && lower.contains("navigate"))
                || (lower.contains("how") && lower.contains("browse"))) {
            return provideNavigationGuide();
        }

        // 3. Greetings
        if (lower.matches("^(hi|hello|hey|greetings|good morning|good afternoon|good evening|who are you|help)(\\b.*|!|\\?)?$")) {
            return new AiChatResponse(
                    "👋 **Hello! Welcome to SS Supermarket!**\n\nI am your instant AI Shopping Assistant. You can ask me:\n" +
                            "• 📖 *'How to use Laptop / Mixer Grinder / Earbuds'* (In-depth usage & care guide)\n" +
                            "• 🧭 *'Help me navigate the whole webpage'* (Full platform tour)\n" +
                            "• ⭐ *'What are your top rated products?'*\n" +
                            "• 💰 *'Find products under ₹500'*\n" +
                            "• 🚚 *'How fast is your delivery?'*\n" +
                            "• 🔍 Ask about any item (e.g. *'Explain boAt Headphones'*)",
                    List.of("🧭 Navigate website", "⭐ Top rated products", "🥦 Fresh produce", "📦 Delivery options")
            );
        }

        // 4. Product Usage or In-depth Explanation query across catalog
        if (lower.contains("how to use") || lower.contains("how do i use") || lower.contains("how do you use")
                || lower.startsWith("explain ") || lower.contains("tell me about ") || lower.contains("benefits of ")) {
            String target = lower
                    .replaceAll("^(how to use|how do i use|how do you use|explain|tell me about|benefits of|what is|guide for)\\s+", "")
                    .replaceAll("\\b(the|a|an|product|item)\\b", "")
                    .trim();
            if (!target.isEmpty()) {
                List<Product> targetMatches = productRepository.searchLegacy(target, null);
                if (targetMatches != null && !targetMatches.isEmpty()) {
                    String finalTarget = target;
                    Product bestMatch = targetMatches.stream()
                            .min(Comparator.comparingInt(p -> {
                                String pName = p.getName().toLowerCase();
                                if (pName.equals(finalTarget)) return 0;
                                if (pName.equals(finalTarget + "s") || pName.equals(finalTarget + "es")) return 1;
                                if (pName.startsWith(finalTarget + " ")) return 2;
                                if (pName.endsWith(" " + finalTarget)) return 3;
                                return 10 + pName.length();
                            }))
                            .orElse(targetMatches.get(0));
                    return explainProduct(bestMatch, lower.contains("how to use") || lower.contains("how do i use"));
                }
            }
        }

        // 5. Ratings / Reviews inquiries
        if (lower.contains("rating") || lower.contains("rated") || lower.contains("star") || lower.contains("review") || lower.contains("best product")) {
            List<Product> all = productRepository.findAll();
            List<Product> topRated = all.stream()
                    .filter(p -> p.getRating() != null && p.getRating() >= 4.5)
                    .sorted(Comparator.comparing(Product::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(4)
                    .toList();

            if (!topRated.isEmpty()) {
                StringBuilder sb = new StringBuilder("⭐ **Customer Top-Rated Products at SS Supermarket:**\n\n");
                for (Product p : topRated) {
                    sb.append("• **").append(p.getName()).append("** — ")
                            .append("⭐ **").append(String.format("%.1f", p.getRating())).append("/5.0** ")
                            .append("(").append(p.getReviewCount() != null ? p.getReviewCount() : 25).append(" reviews) — ")
                            .append("₹").append(p.getPrice())
                            .append(p.getStock() > 0 ? " *(In Stock)*" : " *(Out of Stock)*")
                            .append("\n");
                }
                sb.append("\nEvery product includes verified buyer ratings out of 5 stars!");
                return new AiChatResponse(sb.toString(), List.of("View all products", "🧭 Navigate website", "Dairy items", "Under ₹100"));
            }
        }

        // 6. Delivery policy
        if (lower.contains("delivery") || lower.contains("shipping") || lower.contains("deliver") || lower.contains("reach") || lower.contains("time")) {
            return new AiChatResponse(
                    "🚚 **SS Supermarket Express Delivery:**\n\n" +
                            "• **Speed**: Fast delivery within **30 to 45 minutes** in all local serviceable areas.\n" +
                            "• **Free Delivery**: Available on orders above **₹499**.\n" +
                            "• **Live Tracking**: Track your order status in real-time from the **My Orders** dashboard (`/orders`).\n" +
                            "• **Packaging**: Fresh produce & cold dairy are packed in insulated, hygienic bags.",
                    List.of("Payment methods", "⭐ Top rated products", "🧭 Navigate website")
            );
        }

        // 7. Payment inquiries
        if (lower.contains("payment") || lower.contains("pay") || lower.contains("upi") || lower.contains("cod") || lower.contains("cash") || lower.contains("card")) {
            return new AiChatResponse(
                    "💳 **Supported Payment Options:**\n\n" +
                            "• **Cash on Delivery (COD)**: Pay securely upon package arrival.\n" +
                            "• **UPI**: Google Pay, PhonePe, Paytm, BHIM.\n" +
                            "• **Debit & Credit Cards**: Visa, MasterCard, RuPay.\n" +
                            "• **Net Banking**: All major Indian banks supported.",
                    List.of("Delivery info", "⭐ Top rated products", "Return policy")
            );
        }

        // 8. Return / Refund inquiries
        if (lower.contains("return") || lower.contains("refund") || lower.contains("exchange") || lower.contains("damage")) {
            return new AiChatResponse(
                    "🔄 **Hassle-Free Return & Refund Policy:**\n\n" +
                            "• **Fresh Groceries & Produce**: 24-hour return policy if freshness or quality does not meet your expectations.\n" +
                            "• **Packaged Goods & Electronics**: 7-day replacement or full refund.\n" +
                            "• **Instant Refunds**: Processed back to your original payment method or store wallet within 2-4 business hours.",
                    List.of("Contact seller", "Delivery info", "⭐ Top rated products")
            );
        }

        // 9. Budget / Under ₹X search
        Pattern pricePattern = Pattern.compile("under\\s*(?:rs\\.?|inr|₹)?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pricePattern.matcher(lower);
        if (matcher.find()) {
            try {
                BigDecimal maxPrice = new BigDecimal(matcher.group(1));
                List<Product> cheap = productRepository.findAll().stream()
                        .filter(p -> p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0)
                        .sorted(Comparator.comparing(Product::getPrice))
                        .limit(4)
                        .toList();

                if (!cheap.isEmpty()) {
                    StringBuilder sb = new StringBuilder("💰 **Affordable Items Under ₹").append(maxPrice).append(":**\n\n");
                    for (Product p : cheap) {
                        sb.append("• **").append(p.getName()).append("** — ₹").append(p.getPrice())
                                .append(" (⭐ ").append(String.format("%.1f", p.getRating() != null ? p.getRating() : 4.5)).append("/5.0)\n");
                    }
                    return new AiChatResponse(sb.toString(), List.of("Under ₹50", "⭐ Top rated items", "Fresh vegetables"));
                }
            } catch (Exception ignored) {}
        }

        // 10. Specific product search or category matching
        List<Product> matches = productRepository.searchLegacy(query, null);
        if (matches == null || matches.isEmpty()) {
            // Try individual keywords
            String[] tokens = lower.split("\\s+");
            for (String token : tokens) {
                if (token.length() >= 3 && !List.of("the", "and", "for", "with", "show", "tell", "what", "have", "you").contains(token)) {
                    matches = productRepository.searchLegacy(token, null);
                    if (matches != null && !matches.isEmpty()) {
                        break;
                    }
                }
            }
        }

        if (matches != null && !matches.isEmpty()) {
            if (matches.size() == 1) {
                return explainProduct(matches.get(0), false);
            }

            StringBuilder sb = new StringBuilder("🛒 Here are matching products from our supermarket:\n\n");
            for (Product p : matches.stream().limit(4).toList()) {
                sb.append("• **").append(p.getName()).append("**\n")
                        .append("   ₹").append(p.getPrice())
                        .append(" | ⭐ ").append(String.format("%.1f", p.getRating() != null ? p.getRating() : 4.5)).append("/5.0 (").append(p.getReviewCount() != null ? p.getReviewCount() : 25).append(" reviews)")
                        .append(p.getStock() > 0 ? " | In Stock (" + p.getStock() + ")" : " | Out of Stock")
                        .append("\n");
            }
            sb.append("\nYou can add any of these to your cart or ask me to explain how to use them!");
            return new AiChatResponse(sb.toString(), List.of("⭐ Top rated products", "🧭 Navigate website", "Delivery info"));
        }

        // 11. Friendly fallback
        return new AiChatResponse(
                "I'm here to help you find and use the best supermarket items! We have electronics, fresh food, dairy, appliances, sports gear, and books.\n\n" +
                        "Try asking:\n" +
                        "• *'How to use Laptop / Yoga Mat / Mixer Grinder?'*\n" +
                        "• *'Help me navigate the whole webpage'* (Site tour)\n" +
                        "• *'What are the highest rated products?'*\n" +
                        "• *'Find items under ₹500'*\n" +
                        "• *'What is your delivery time?'*",
                List.of("🧭 Navigate website", "⭐ Top rated products", "🥛 Dairy items", "🚚 Delivery info")
        );
    }

    private AiChatResponse explainProduct(Product p, boolean emphasizeUsage) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛍️ **").append(p.getName()).append("**\n\n");

        sb.append("🏷️ **Category**: ").append(p.getCategory());
        if (p.getBrand() != null && !p.getBrand().isBlank()) {
            sb.append(" | **Brand**: ").append(p.getBrand());
        }
        sb.append("\n");

        sb.append("💰 **Price**: ₹").append(p.getPrice())
                .append(" | ⭐ **Rating**: ").append(String.format("%.1f", p.getRating() != null ? p.getRating() : 4.5)).append("/5.0 ")
                .append("(").append(p.getReviewCount() != null ? p.getReviewCount() : 25).append(" customer reviews)\n");

        sb.append("📦 **Stock Status**: ")
                .append(p.getStock() > 0 ? "✅ In stock (" + p.getStock() + " units available)" : "❌ Currently Out of stock")
                .append("\n\n");

        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            sb.append("📝 **Product Overview & Key Features:**\n")
                    .append(p.getDescription()).append("\n\n");
        }

        sb.append("📖 **How to Use & Best Practices:**\n");
        sb.append(generateUsageInstructions(p));
        sb.append("\n\n");

        sb.append("🚚 **Delivery & Purchase Advice:**\n");
        sb.append("• Eligible for **30–45 minute express delivery** in all serviceable areas.\n");
        sb.append("• Free delivery on orders above **₹499**.\n");
        if (p.getStock() > 0) {
            sb.append("• Click **Add to Cart** on this page to order instantly, or adjust quantity as needed.");
        } else {
            sb.append("• This item is temporarily out of stock. Check back soon or ask me for similar alternatives!");
        }

        return new AiChatResponse(
                sb.toString(),
                List.of("⭐ Top rated products", "🧭 Navigate website", "🚚 Delivery info", "Similar items")
        );
    }

    private String generateUsageInstructions(Product p) {
        String cat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
        String name = p.getName() != null ? p.getName().toLowerCase() : "";

        if (name.contains("stand")) {
            return "1. **Adjust Angle & Elevation**: Position the stand so your display aligns with eye level for optimal posture.\n" +
                    "2. **Stability Check**: Verify non-slip silicone pads are securely seated on your desk surface.\n" +
                    "3. **Airflow Ventilation**: Ensure the open ventilated cutout aligns with laptop bottom vents to prevent throttling.\n" +
                    "4. **Fold & Travel**: Fold completely flat into your sleeve or backpack when on the go.";
        } else if (cat.contains("accessory") || cat.contains("accessories") || name.contains("headphone")
                || name.contains("earbud") || name.contains("speaker") || name.contains("neckband")
                || name.contains("mouse") || name.contains("keyboard") || name.contains("mic")
                || name.contains("hub") || name.contains("cable")) {
            return "1. **Connectivity**: Turn on Bluetooth or plug the USB connector firmly into your computer or mobile.\n" +
                    "2. **Ergonomic Positioning**: Adjust headband, ear tips, or wrist resting angles for optimal comfort.\n" +
                    "3. **Audio / Input Settings**: Fine-tune volume levels or custom DPI/RGB modes to suit your work or gaming style.\n" +
                    "4. **Storage & Care**: Store in a protective case when not in use; keep away from excessive moisture.";
        } else if (cat.contains("wearable") || name.contains("watch") || name.contains("band")) {
            return "1. **Pairing**: Install the official companion app on your smartphone and pair via Bluetooth.\n" +
                    "2. **Wear Snugly**: Position the sensor 1 finger width above your wrist bone for accurate heart rate/SpO2 readings.\n" +
                    "3. **Workout Modes**: Select matching exercise presets before beginning your routine for precise tracking.\n" +
                    "4. **Care**: Wipe sweat from the optical sensors after workouts and keep charging pins clean.";
        } else if (cat.contains("electronic") || name.contains("laptop") || (name.contains("phone") && !name.contains("headphone"))
                || name.contains("tv") || name.contains("tablet") || name.contains("power bank") || name.contains("webcam")) {
            return "1. **Unbox & Initial Charge**: Charge to 100% using the manufacturer-certified charger before first use.\n" +
                    "2. **Initial Setup**: Power on and follow the on-screen configuration (Wi-Fi, security & account sync).\n" +
                    "3. **Thermal Care**: Keep cooling vents unobstructed and operate on flat, firm surfaces to avoid overheating.\n" +
                    "4. **Battery Health**: Maintain battery levels between 20% and 80% for prolonged lifecycle longevity.";
        } else if (cat.contains("kitchen") || cat.contains("home") || name.contains("kettle")
                || name.contains("mixer") || name.contains("scale") || name.contains("lamp") || name.contains("pillow")) {
            return "1. **First-Time Preparation**: Rinse food-contact jars or kettle body thoroughly with warm water before first use.\n" +
                    "2. **Safe Placement**: Place on a level, heat-resistant surface away from running water or sink edges.\n" +
                    "3. **Capacity Limits**: Do not exceed maximum fill lines; ensure safety lids are tightly locked before powering on.\n" +
                    "4. **Maintenance**: Unplug after use. Wipe the exterior with a damp cloth; never submerge the electrical motor base.";
        } else if (cat.contains("book") || name.contains("book") || name.contains("handbook")) {
            return "1. **Structured Learning**: Begin with fundamental concept reviews and chapter summaries.\n" +
                    "2. **Hands-On Practice**: Implement the code examples and database schema queries directly on your computer.\n" +
                    "3. **Exercise Challenges**: Work through the end-of-chapter exercises and algorithmic problem sets.\n" +
                    "4. **Review & Revision**: Bookmark key reference sheets and revision notes for rapid interview recall.";
        } else if (cat.contains("fashion") || name.contains("shirt") || name.contains("shoe")
                || name.contains("bag") || name.contains("backpack")) {
            return "1. **Fit & Ergonomics**: Adjust straps, laces, or sizing for balanced posture and all-day comfort.\n" +
                    "2. **Washing & Cleaning**: Wash fabrics inside-out in cold water with gentle detergent; air dry in shaded areas.\n" +
                    "3. **Footwear Care**: Wipe sneakers with a soft damp cloth; use shoe trees or paper stuffing to retain shape.\n" +
                    "4. **Storage**: Store in a dry, ventilated closet away from dampness or direct sunlight.";
        } else if (cat.contains("sport") || cat.contains("fitness") || name.contains("yoga")
                || name.contains("dumbbell") || name.contains("roller")) {
            return "1. **Warm-Up First**: Spend 5 minutes on light dynamic stretching before beginning weight or core training.\n" +
                    "2. **Correct Posture**: Maintain proper spinal alignment and controlled breathing throughout every repetition.\n" +
                    "3. **Progressive Routine**: Start with comfortable weight/reps and gradually increase intensity over time.\n" +
                    "4. **Hygiene & Storage**: Wipe equipment with a disinfectant wipe after every workout session; store flat.";
        } else {
            return "1. **Freshness Verification**: Check packaging seals upon delivery for optimal freshness and quality guarantee.\n" +
                    "2. **Proper Storage**: Store perishable dairy and produce chilled (1°C–4°C); keep dry goods in airtight containers.\n" +
                    "3. **Preparation & Culinary Use**: Wash thoroughly under running water, slice fresh, or incorporate into your favorite recipes.\n" +
                    "4. **Best Before**: Consume within the recommended freshness window for peak nutritional value and taste.";
        }
    }

    private AiChatResponse provideNavigationGuide() {
        return new AiChatResponse(
                "🧭 **SS Supermarket Complete Webpage Navigation Guide:**\n\n" +
                        "Here is how to navigate and make the most of every section of our platform:\n\n" +
                        "1. 🏠 **Home Page (`/`)**\n" +
                        "   • View trending supermarket products, promotional banners, and quick category shortcuts.\n" +
                        "   • Jump straight into shopping with the **Shop Now** button.\n\n" +
                        "2. 🔍 **Products Catalog (`/products`)**\n" +
                        "   • **Search Bar**: Search by keyword (e.g. *Laptop*, *Milk*, *Earbuds*).\n" +
                        "   • **Category Tree**: Filter by department (Electronics, Fashion, Home & Kitchen, Books, Sports, etc.).\n" +
                        "   • **Live Information**: Compare prices in ₹, stock counts, seller phone numbers, and verified ratings out of 5 stars.\n\n" +
                        "3. 🛍️ **Product Details Page (`/products/{id}`)**\n" +
                        "   • High-definition product photos, detailed descriptions, and seller contacts.\n" +
                        "   • Select desired item quantity and click **Add to Cart**.\n" +
                        "   • The AI chatbot automatically detects the product you are viewing for instant explanations!\n\n" +
                        "4. 🛒 **Shopping Cart (`/cart`)**\n" +
                        "   • Review added items, update quantities, view subtotals, or remove items.\n" +
                        "   • Click **Proceed to Checkout** when ready to order.\n\n" +
                        "5. 💳 **Checkout & Payment (`/checkout`)**\n" +
                        "   • Select your saved shipping address or enter a new delivery location.\n" +
                        "   • Choose between **Cash on Delivery (COD)**, **UPI**, or **Card / Net Banking**.\n" +
                        "   • Instant order confirmation.\n\n" +
                        "6. 📦 **My Orders & Tracking (`/orders`)**\n" +
                        "   • Track live progress of your orders (*Confirmed*, *Processing*, *Shipped*, *Delivered*).\n" +
                        "   • View full order breakdown and digital invoices (`/orders/{id}`).\n\n" +
                        "7. 👤 **Profile & Seller Application (`/profile`)**\n" +
                        "   • Update profile name, phone number, and address book.\n" +
                        "   • Buyers can click **Become Seller** to start selling products on SS Supermarket.\n\n" +
                        "8. 🏪 **Seller Portal (`/seller`)** *(For Verified Sellers)*\n" +
                        "   • Add new products, adjust inventory stock, set prices, and fulfill customer orders.\n\n" +
                        "9. 🛡️ **Admin Portal (`/admin`)** *(For Admins)*\n" +
                        "   • Platform-wide monitoring, user role management (Buyer/Seller/Admin), and catalog maintenance.\n\n" +
                        "💡 *Tip: You can ask me anytime to explain any product or find deals under ₹500!*",
                List.of("Browse products", "⭐ Top rated products", "🚚 Delivery info", "Payment methods")
        );
    }

    private Product extractProductFromHistory(List<AiChatMessageDto> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        Pattern botProductPattern = Pattern.compile("🛍️\\s*\\*\\*(.*?)\\*\\*");
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatMessageDto msg = history.get(i);
            if (msg == null || msg.getContent() == null) {
                continue;
            }
            // Check bot responses containing product title
            Matcher m = botProductPattern.matcher(msg.getContent());
            if (m.find()) {
                String candidateName = m.group(1).trim();
                List<Product> matches = productRepository.searchLegacy(candidateName, null);
                if (matches != null && !matches.isEmpty()) {
                    return matches.get(0);
                }
            }
            // Check user messages containing product queries
            if ("user".equalsIgnoreCase(msg.getRole())) {
                String userQuery = msg.getContent().trim();
                if (userQuery.length() >= 3) {
                    List<Product> matches = productRepository.searchLegacy(userQuery, null);
                    if (matches != null && !matches.isEmpty()) {
                        return matches.get(0);
                    }
                }
            }
        }
        return null;
    }

    private AiChatResponse summarizeChatHistory(List<AiChatMessageDto> history) {
        if (history == null || history.isEmpty()) {
            return new AiChatResponse(
                    "📜 **Chat History:**\n\n" +
                            "We've just started our session! You haven't sent any earlier messages yet.\n\n" +
                            "You can ask me:\n" +
                            "• *'How to use Laptop / Earbuds / Yoga Mat?'*\n" +
                            "• *'Help me navigate the whole webpage'* (Full tour)\n" +
                            "• *'What are your top rated items?'*",
                    List.of("🧭 Navigate website", "⭐ Top rated products", "🚚 Delivery info")
            );
        }

        StringBuilder sb = new StringBuilder("📜 **Recent Chat History Summary:**\n\n");
        int start = Math.max(0, history.size() - 6);
        for (int i = start; i < history.size(); i++) {
            AiChatMessageDto item = history.get(i);
            if (item == null || item.getContent() == null) continue;
            String sender = "user".equalsIgnoreCase(item.getRole()) ? "👤 **You**" : "🤖 **AI**";
            String text = item.getContent().replaceAll("<[^>]*>", "").replaceAll("\\n+", " ").trim();
            if (text.length() > 90) {
                text = text.substring(0, 87) + "...";
            }
            sb.append("• ").append(sender).append(": ").append(text).append("\n");
        }
        sb.append("\n💡 *Your chat history is saved automatically as you browse the website. Click the 🗑️ icon at the top of the chat window anytime to clear it!*");
        return new AiChatResponse(
                sb.toString(),
                List.of("🧭 Navigate website", "⭐ Top rated products", "Similar items")
        );
    }
}
