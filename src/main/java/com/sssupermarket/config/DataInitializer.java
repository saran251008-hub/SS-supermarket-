package com.sssupermarket.config;
import com.sssupermarket.entity.Product;
import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
import com.sssupermarket.entity.Category;
import com.sssupermarket.repository.CategoryRepository;
import com.sssupermarket.repository.ProductRepository;
import com.sssupermarket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    public static final String ADMIN_EMAIL = "admin@sssupermarket.com";
    public static final String ADMIN_PASSWORD = "Admin@123";
        public static final String SELLER_EMAIL = "seller@sssupermarket.com";
        public static final String SELLER_PASSWORD = "Seller@123";
        public static final String SELLER_PHONE = "9876543210";
    @Bean
    public CommandLineRunner seedDatabase(UserRepository userRepository,
                                          ProductRepository productRepository,
                                          CategoryRepository categoryRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
                userRepository.save(new User("SS Supermarket Admin", ADMIN_EMAIL,
                        passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN));
                log.info("Created default admin account: {}", ADMIN_EMAIL);
            }

            User existingSeller = userRepository.findByEmail(SELLER_EMAIL).orElseGet(() -> {
                User seededSeller = new User("SS Supermarket Seller", SELLER_EMAIL,
                        passwordEncoder.encode(SELLER_PASSWORD), Role.SELLER);
                seededSeller.setPhone(SELLER_PHONE);
                return userRepository.save(seededSeller);
            });
            if (!SELLER_PHONE.equals(existingSeller.getPhone())) {
                existingSeller.setPhone(SELLER_PHONE);
                userRepository.save(existingSeller);
            }
            final User seller = existingSeller;
            if (productRepository.count() == 0) {
                productRepository.saveAll(List.of(
                        new Product("Laptop",
                                "High performance laptop with 16 GB RAM and 512 GB SSD. Perfect for work and study.",
                                new BigDecimal("55000"), "Electronics",
                                "/images/products/laptop.jpg", 10, seller.getId()),
                        new Product("Smartphone",
                                "6.5 inch AMOLED display smartphone with 128 GB storage and dual camera.",
                                new BigDecimal("19500"), "Electronics",
                                "/images/products/smartphone.jpg", 20, seller.getId()),
                        new Product("Headphones",
                                "Wireless over-ear headphones with active noise cancellation and 30 hour battery.",
                                new BigDecimal("2000"), "Accessories",
                                "/images/products/headphones.jpg", 30, seller.getId()),
                        new Product("Keyboard",
                                "Mechanical keyboard with backlight and quiet switches for comfortable typing.",
                                new BigDecimal("1300"), "Accessories",
                                "/images/products/Keyboard.jpg", 25, seller.getId()),
                        new Product("Smart Watch",
                                "Smart watch with heart rate monitor, GPS and 7 day battery life.",
                                new BigDecimal("4000"), "Wearables",
                                "/images/products/smartwatch.jpg", 15, seller.getId())
                ));
                log.info("Seeded {} sample products.", productRepository.count());
                        } else {
                                updateLegacyImageUrls(productRepository);
                                productRepository.findAll().stream()
                                                .filter(product -> product.getSellerId() == null)
                                                .forEach(product -> {
                                                        product.setSellerId(seller.getId());
                                                        productRepository.save(product);
                                                });
            }
                        seedCategories(categoryRepository, productRepository);
        };
    }

        private void seedCategories(CategoryRepository repository, ProductRepository productRepository) {
                List<String> topNames = List.of("Electronics", "Fashion", "Home & Kitchen", "Books", "Toys & Games",
                                "Sports & Fitness", "Beauty & Personal Care", "Grocery & Food", "Automotive",
                                "Stationery & Office", "Arts, Crafts & Hobbies");
                java.util.Map<String, Category> categories = new java.util.HashMap<>();
                topNames.forEach(name -> categories.put(name, category(repository, name, null)));

                Category books = categories.get("Books");
                Category engineering = category(repository, "Engineering", books);
                Category computerScience = category(repository, "Computer Science", engineering);
                for (String name : List.of("Artificial Intelligence", "Machine Learning", "Deep Learning",
                                "Data Structures & Algorithms", "Database Management Systems", "Operating Systems",
                                "Computer Networks", "System Design", "Cloud Computing", "Cybersecurity", "NLP", "Computer Vision")) {
                        category(repository, name, computerScience);
                }
                Category ece = category(repository, "Electronics & Communication Engineering", engineering);
                for (String name : List.of("Digital Electronics", "Analog Electronics", "Signals & Systems",
                                "Communication Systems", "Embedded Systems")) category(repository, name, ece);
                Category eee = category(repository, "Electrical & Electronics Engineering", engineering);
                for (String name : List.of("Electrical Machines", "Power Systems", "Power Electronics", "Control Systems"))
                        category(repository, name, eee);
                Category mechanical = category(repository, "Mechanical Engineering", engineering);
                for (String name : List.of("Thermodynamics", "Fluid Mechanics", "Machine Design", "Manufacturing"))
                        category(repository, name, mechanical);
                Category civil = category(repository, "Civil Engineering", engineering);
                for (String name : List.of("Structural Engineering", "Geotechnical Engineering", "Transportation Engineering", "Environmental Engineering"))
                        category(repository, name, civil);
                for (String name : List.of("Leather Technology", "Food Technology", "Biomedical Engineering"))
                        category(repository, name, engineering);
                Category medical = category(repository, "Medical", books);
                for (String name : List.of("Anatomy", "Physiology", "Pharmacology", "Pathology", "Surgery", "Nursing"))
                        category(repository, name, medical);
                Category science = category(repository, "Science", books);
                for (String name : List.of("Physics", "Chemistry", "Mathematics", "Biology")) category(repository, name, science);
                for (String name : List.of("Sociology", "Geography", "Political Science", "History")) category(repository, name, books);
                Category electronics = categories.get("Electronics");
                Category mobiles = category(repository, "Mobiles", electronics);
                category(repository, "Smartphones", mobiles);
                category(repository, "Feature Phones", mobiles);
                category(repository, "Mobile Accessories", mobiles);
                Category computers = category(repository, "Computers", electronics);
                Category laptops = category(repository, "Laptops", computers);
                category(repository, "Desktops", computers);
                category(repository, "Monitors", computers);
                category(repository, "Computer Accessories", computers);
                Category components = category(repository, "Computer Components", electronics);
                for (String name : List.of("CPU", "GPU", "RAM", "Motherboard", "SSD")) category(repository, name, components);
                Category audio = category(repository, "Audio", electronics);
                for (String name : List.of("Headphones", "Earbuds", "Speakers", "Microphones")) category(repository, name, audio);
                category(repository, "Cameras", electronics);
                category(repository, "Headphones", category(repository, "Audio", electronics));
                category(repository, "Men's Fashion", categories.get("Fashion"));
                category(repository, "Women's Fashion", categories.get("Fashion"));
                category(repository, "Kids' Fashion", categories.get("Fashion"));
                category(repository, "Kitchen", categories.get("Home & Kitchen"));
                category(repository, "Kitchen Appliances", categories.get("Home & Kitchen"));
                for (String name : List.of("Home Appliances", "Furniture", "Home Decor", "Home Improvement"))
                        category(repository, name, categories.get("Home & Kitchen"));
                for (String name : List.of("Educational Toys", "Building Toys", "Board Games", "Puzzles", "Remote Controlled Toys"))
                        category(repository, name, categories.get("Toys & Games"));
                for (String name : List.of("Cricket", "Football", "Badminton", "Tennis", "Running", "Gym & Fitness", "Cycling"))
                        category(repository, name, categories.get("Sports & Fitness"));
                for (String name : List.of("Skin Care", "Hair Care", "Makeup", "Personal Care", "Fragrance"))
                        category(repository, name, categories.get("Beauty & Personal Care"));
                for (String name : List.of("Staples", "Snacks", "Beverages", "Cooking", "Packaged Food"))
                        category(repository, name, categories.get("Grocery & Food"));

                productRepository.findAll().forEach(product -> {
                        if (product.getCategoryEntity() != null) return;
                        Category target = switch (product.getName()) {
                                case "Laptop" -> laptops;
                                case "Smartphone" -> category(repository, "Smartphones", mobiles);
                                case "Headphones" -> category(repository, "Headphones", category(repository, "Audio", electronics));
                                case "Keyboard" -> category(repository, "Computer Accessories", computers);
                                case "Smart Watch" -> category(repository, "Wearables", electronics);
                                default -> categories.getOrDefault(product.getCategory(), electronics);
                        };
                        product.setCategoryEntity(target);
                        productRepository.save(product);
                });
        }

        private Category category(CategoryRepository repository, String name, Category parent) {
                String slug = slug(name, parent);
                return repository.findBySlug(slug).orElseGet(() -> {
                        Category item = new Category();
                        item.setName(name);
                        item.setSlug(slug);
                        item.setParent(parent);
                        item.setLevel(parent == null ? 0 : parent.getLevel() + 1);
                        return repository.save(item);
                });
        }

        private String slug(String name, Category parent) {
                String prefix = parent == null ? "" : parent.getSlug() + "-";
                return prefix + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        }

        private void updateLegacyImageUrls(ProductRepository productRepository) {
                productRepository.findAll().forEach(product -> {
                        String imageUrl = switch (product.getName()) {
                                case "Laptop" -> "/images/products/laptop.jpg";
                                case "Smartphone" -> "/images/products/smartphone.jpg";
                                case "Headphones" -> "/images/products/headphones.jpg";
                                case "Keyboard" -> "/images/products/Keyboard.jpg";
                                case "Smart Watch" -> "/images/products/smartwatch.jpg";
                                default -> null;
                        };

                        String currentImageUrl = product.getImageUrl();
                        boolean isLegacyImage = currentImageUrl == null
                                        || !currentImageUrl.toLowerCase().endsWith(".jpg")
                                        || currentImageUrl.equalsIgnoreCase("/images/products/keyboard.jpg");
                        if (imageUrl != null && isLegacyImage && !imageUrl.equals(currentImageUrl)) {
                                product.setImageUrl(imageUrl);
                                productRepository.save(product);
                                log.info("Updated image URL for {} to {}", product.getName(), imageUrl);
                        }
                });
        }
}
