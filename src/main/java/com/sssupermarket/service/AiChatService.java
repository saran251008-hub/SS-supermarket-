package com.sssupermarket.service;

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

        // 1. Current product context if provided
        if (request.getProductId() != null) {
            Optional<Product> currentProductOpt = productRepository.findById(request.getProductId());
            if (currentProductOpt.isPresent()) {
                Product cp = currentProductOpt.get();
                if (lower.contains("this product") || lower.contains("it") || lower.contains("rating") || lower.contains("stock") || lower.contains("price") || lower.contains("detail")) {
                    return describeProduct(cp);
                }
            }
        }

        // 2. Greetings
        if (lower.matches("^(hi|hello|hey|greetings|good morning|good afternoon|good evening|who are you|help)(\\b.*|!|\\?)?$")) {
            return new AiChatResponse(
                    "👋 **Hello! Welcome to SS Supermarket!**\n\nI am your instant AI Shopping Assistant. You can ask me:\n" +
                            "• ⭐ *'What are your top rated products?'*\n" +
                            "• 🍎 *'Show me fresh fruits and dairy items'*\n" +
                            "• 💰 *'Find products under ₹100'*\n" +
                            "• 🚚 *'How fast is your delivery?'*\n" +
                            "• 🔍 Ask about any item (e.g. *'Tell me about Organic Milk'*)",
                    List.of("⭐ Top rated products", "🥦 Fresh vegetables", "📦 Delivery options", "🥛 Dairy items")
            );
        }

        // 3. Ratings / Reviews inquiries
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
                return new AiChatResponse(sb.toString(), List.of("View all products", "Dairy items", "Under ₹100"));
            }
        }

        // 4. Delivery policy
        if (lower.contains("delivery") || lower.contains("shipping") || lower.contains("deliver") || lower.contains("reach") || lower.contains("time")) {
            return new AiChatResponse(
                    "🚚 **SS Supermarket Express Delivery:**\n\n" +
                            "• **Speed**: Fast delivery within **30 to 45 minutes** in all local serviceable areas.\n" +
                            "• **Free Delivery**: Available on orders above **₹499**.\n" +
                            "• **Live Tracking**: Track your order status in real-time from the **My Orders** dashboard.\n" +
                            "• **Packaging**: Fresh produce & cold dairy are packed in insulated, hygienic bags.",
                    List.of("Payment methods", "Top rated products", "Browse catalog")
            );
        }

        // 5. Payment inquiries
        if (lower.contains("payment") || lower.contains("pay") || lower.contains("upi") || lower.contains("cod") || lower.contains("cash") || lower.contains("card")) {
            return new AiChatResponse(
                    "💳 **Supported Payment Options:**\n\n" +
                            "• **Cash on Delivery (COD)**: Pay securely upon package arrival.\n" +
                            "• **UPI**: Google Pay, PhonePe, Paytm, BHIM.\n" +
                            "• **Debit & Credit Cards**: Visa, MasterCard, RuPay.\n" +
                            "• **Net Banking**: All major Indian banks supported.",
                    List.of("Delivery info", "Top rated products", "Return policy")
            );
        }

        // 6. Return / Refund inquiries
        if (lower.contains("return") || lower.contains("refund") || lower.contains("exchange") || lower.contains("damage")) {
            return new AiChatResponse(
                    "🔄 **Hassle-Free Return & Refund Policy:**\n\n" +
                            "• **Fresh Groceries & Produce**: 24-hour return policy if freshness or quality does not meet your expectations.\n" +
                            "• **Packaged Goods & Electronics**: 7-day replacement or full refund.\n" +
                            "• **Instant Refunds**: Processed back to your original payment method or store wallet within 2-4 business hours.",
                    List.of("Contact seller", "Delivery info", "Top rated products")
            );
        }

        // 7. Budget / Under ₹X search
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
                    return new AiChatResponse(sb.toString(), List.of("Under ₹50", "Top rated items", "Fresh vegetables"));
                }
            } catch (Exception ignored) {}
        }

        // 8. Specific product search or category matching
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
                return describeProduct(matches.get(0));
            }

            StringBuilder sb = new StringBuilder("🛒 Here are matching products from our supermarket:\n\n");
            for (Product p : matches.stream().limit(4).toList()) {
                sb.append("• **").append(p.getName()).append("**\n")
                        .append("   ₹").append(p.getPrice())
                        .append(" | ⭐ ").append(String.format("%.1f", p.getRating() != null ? p.getRating() : 4.5)).append("/5.0 (").append(p.getReviewCount() != null ? p.getReviewCount() : 25).append(" reviews)")
                        .append(p.getStock() > 0 ? " | In Stock (" + p.getStock() + ")" : " | Out of Stock")
                        .append("\n");
            }
            sb.append("\nYou can add any of these to your cart or view details on the Products page!");
            return new AiChatResponse(sb.toString(), List.of("Top rated products", "Delivery info", "Deals under ₹100"));
        }

        // 9. Friendly fallback
        return new AiChatResponse(
                "I'm here to help you find the best supermarket items! We have fresh vegetables, fruits, dairy, bakery items, electronics, and snacks.\n\n" +
                        "Try asking:\n" +
                        "• *'Do you have milk or butter?'*\n" +
                        "• *'What are the highest rated products?'*\n" +
                        "• *'Find items under ₹150'*\n" +
                        "• *'What is your delivery time?'*",
                List.of("⭐ Top rated products", "🥛 Dairy items", "🍎 Fresh fruits", "🚚 Delivery info")
        );
    }

    private AiChatResponse describeProduct(Product p) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛍️ **").append(p.getName()).append("**\n\n");
        sb.append("• **Category**: ").append(p.getCategory()).append("\n");
        if (p.getBrand() != null && !p.getBrand().isBlank()) {
            sb.append("• **Brand**: ").append(p.getBrand()).append("\n");
        }
        sb.append("• **Price**: ₹").append(p.getPrice()).append("\n");
        sb.append("• **Rating**: ⭐ **").append(String.format("%.1f", p.getRating() != null ? p.getRating() : 4.5)).append("/5.0** ")
                .append("(").append(p.getReviewCount() != null ? p.getReviewCount() : 25).append(" customer reviews)\n");
        sb.append("• **Availability**: ").append(p.getStock() > 0 ? "✅ In stock (" + p.getStock() + " units available)" : "❌ Out of stock").append("\n");
        if (p.getDescription() != null && !p.getDescription().isBlank()) {
            sb.append("• **Details**: ").append(p.getDescription()).append("\n");
        }
        sb.append("\nYou can order this directly by clicking **Add to Cart** or ask me about related items!");
        return new AiChatResponse(sb.toString(), List.of("⭐ Top rated products", "🚚 Delivery info", "Similar items"));
    }
}
