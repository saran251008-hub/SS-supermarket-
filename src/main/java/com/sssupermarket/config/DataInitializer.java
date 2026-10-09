package com.sssupermarket.config;

import com.sssupermarket.entity.Category;
import com.sssupermarket.entity.Product;
import com.sssupermarket.entity.Role;
import com.sssupermarket.entity.User;
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

            seedProducts(productRepository, seller);
            updateLegacyImageUrls(productRepository);
            seedCategories(categoryRepository, productRepository);
        };
    }

    private void seedProducts(ProductRepository productRepository, User seller) {
        Long sellerId = seller.getId();
        List<Product> defaults = List.of(
                new Product("Laptop", "High performance laptop with 16 GB RAM and 512 GB SSD. Perfect for work and study.", new BigDecimal("45000"), "Electronics", "/images/products/laptop.jpg", 10, sellerId, "HP"),
                new Product("Smartphone", "6.5 inch AMOLED display smartphone with 128 GB storage and dual camera.", new BigDecimal("18000"), "Electronics", "/images/products/smartphone.jpg", 20, sellerId, "Samsung"),
                new Product("Headphones", "Wireless over-ear headphones with active noise cancellation and 30 hour battery.", new BigDecimal("2500"), "Accessories", "/images/products/headphones.jpg", 30, sellerId, "boAt"),
                new Product("Keyboard", "Mechanical keyboard with backlight and quiet switches for comfortable typing.", new BigDecimal("1500"), "Accessories", "/images/products/Keyboard.jpg", 25, sellerId, "Redragon"),
                new Product("Smart Watch", "Smart watch with heart rate monitor, GPS and 7 day battery life.", new BigDecimal("5000"), "Wearables", "/images/products/smartwatch.jpg", 15, sellerId, "Fire-Boltt"),
                new Product("Gaming Mouse", "Ergonomic wireless gaming mouse with RGB backlight and adjustable DPI.", new BigDecimal("1800"), "Electronics", "/images/products/gaming-mouse.jpg", 15, sellerId, "Logitech"),
                new Product("Gaming Laptop", "High performance gaming laptop with 16GB RAM, RTX Graphics and 1TB SSD.", new BigDecimal("75000"), "Electronics", "/images/products/gaming-laptop.jpg", 10, sellerId, "ASUS"),
                new Product("Smart TV", "4K Ultra HD Smart LED television with Dolby Audio and smart streaming apps.", new BigDecimal("42000"), "Electronics", "/images/products/smart-tv.jpg", 12, sellerId, "Samsung"),
                new Product("Bluetooth Speaker", "Portable Bluetooth speaker with deep bass and IPX7 water resistance.", new BigDecimal("2200"), "Electronics", "/images/products/speaker.jpg", 25, sellerId, "JBL"),
                new Product("Tablet", "10-inch HD display tablet suitable for online classes, streaming and reading.", new BigDecimal("16000"), "Electronics", "/images/products/tablet.jpg", 15, sellerId, "Lenovo"),
                new Product("Webcam", "Full HD 1080p webcam with built-in noise-cancelling microphone for meetings.", new BigDecimal("1800"), "Electronics", "/images/products/webcam.jpg", 20, sellerId, "Logitech"),
                new Product("Power Bank", "20000mAh fast charging portable power bank with dual USB output.", new BigDecimal("1800"), "Electronics", "/images/products/power-bank.jpg", 30, sellerId, "Anker"),
                new Product("Wireless Mouse", "Compact ergonomic wireless mouse with silent clicks and 2.4GHz receiver.", new BigDecimal("900"), "Accessories", "/images/products/mouse.jpg", 40, sellerId, "Logitech"),
                new Product("USB Hub", "7-in-1 USB-C hub with HDMI 4K output, SD card reader and USB 3.0 ports.", new BigDecimal("1200"), "Accessories", "/images/products/usb-hub.jpg", 18, sellerId, "Portronics"),
                new Product("Laptop Backpack", "Water-resistant laptop backpack with padded laptop sleeve and anti-theft design.", new BigDecimal("1500"), "Accessories", "/images/products/backpack.jpg", 22, sellerId, "HP"),
                new Product("USB Microphone", "Studio condenser USB microphone with tripod stand for streaming and podcasts.", new BigDecimal("2800"), "Accessories", "/images/products/microphone.jpg", 14, sellerId, "FIFINE"),
                new Product("Charging Cable", "Braided fast charging USB Type-C cable with durable aluminum connectors.", new BigDecimal("350"), "Accessories", "/images/products/charging-cable.jpg", 50, sellerId, "boAt"),
                new Product("Laptop Stand", "Foldable ergonomic aluminum laptop riser stand with heat dissipation.", new BigDecimal("1100"), "Accessories", "/images/products/laptop-stand.jpg", 16, sellerId, "Portronics"),
                new Product("Fitness Band", "Color display smart fitness tracker with heart rate and sleep monitoring.", new BigDecimal("2500"), "Wearables", "/images/products/fitness-band.jpg", 20, sellerId, "Xiaomi"),
                new Product("Smart Watch Pro", "Advanced smartwatch with Bluetooth calling, AMOLED display and 100+ sports modes.", new BigDecimal("6500"), "Wearables", "/images/products/smartwatch-pro.jpg", 13, sellerId, "Noise"),
                new Product("Wireless Earbuds", "True wireless stereo earbuds with active noise cancellation and IPX5 rating.", new BigDecimal("2200"), "Wearables", "/images/products/earbuds.jpg", 28, sellerId, "boAt"),
                new Product("Bluetooth Neckband", "Magnetic Bluetooth neckband earphones with fast charge and vibration alerts.", new BigDecimal("1300"), "Wearables", "/images/products/neckband.jpg", 24, sellerId, "OnePlus"),
                new Product("Electric Kettle", "1.8L Stainless steel electric kettle with rapid boiling and auto shut-off.", new BigDecimal("1200"), "Home & Kitchen", "/images/products/kettle.jpg", 17, sellerId, "Prestige"),
                new Product("Mixer Grinder", "750W Heavy duty mixer grinder with 3 stainless steel jars.", new BigDecimal("2800"), "Home & Kitchen", "/images/products/mixer.jpg", 11, sellerId, "Philips"),
                new Product("Desk Lamp", "Eye-caring rechargeable LED study lamp with 3 color temperatures.", new BigDecimal("750"), "Home & Kitchen", "/images/products/desk-lamp.jpg", 26, sellerId, "Wipro"),
                new Product("Digital Scale", "High precision digital weighing scale with tempered glass platform.", new BigDecimal("900"), "Home & Kitchen", "/images/products/weighing-scale.jpg", 15, sellerId, "Hoffen"),
                new Product("Python Programming Book", "Complete beginner to advanced guide for modern Python programming.", new BigDecimal("650"), "Books", "/images/products/python-book.jpg", 35, sellerId, "O'Reilly"),
                new Product("Machine Learning Book", "Hands-on machine learning with Scikit-Learn, Keras, and TensorFlow.", new BigDecimal("850"), "Books", "/images/products/ml-book.jpg", 20, sellerId, "O'Reilly"),
                new Product("DSA Handbook", "Comprehensive guide to Data Structures and Algorithms with interview problems.", new BigDecimal("550"), "Books", "/images/products/dsa-book.jpg", 30, sellerId, "Pearson"),
                new Product("DBMS Book", "Fundamentals of Database Systems: SQL, Relational Design, and Transactions.", new BigDecimal("480"), "Books", "/images/products/dbms-book.jpg", 22, sellerId, "McGraw Hill"),
                new Product("Cotton T-Shirt", "100% Bio-washed breathable cotton crew neck t-shirt for daily comfort.", new BigDecimal("499"), "Fashion", "/images/products/tshirt.jpg", 39, sellerId, "Puma"),
                new Product("College Backpack", "Trendy lightweight backpack with multi-pocket storage for college students.", new BigDecimal("999"), "Fashion", "/images/products/casual-bag.jpg", 18, sellerId, "Skybags"),
                new Product("Running Shoes", "Lightweight breathable mesh athletic running sneakers with shock absorption.", new BigDecimal("1800"), "Fashion", "/images/products/running-shoes.jpg", 16, sellerId, "Nike"),
                new Product("Yoga Mat", "Extra thick 6mm high-density non-slip exercise yoga mat with carry strap.", new BigDecimal("700"), "Sports & Fitness", "/images/products/yoga-mat.jpg", 20, sellerId, "Boldfit"),
                new Product("Dumbbell Set", "Hexagonal rubber coated dumbbell weight set for home gym workouts.", new BigDecimal("2200"), "Sports & Fitness", "/images/products/dumbbells.jpg", 12, sellerId, "Kore"),
                new Product("RGB Gaming Keyboard", "Tenkeyless mechanical gaming keyboard with customizable RGB backlighting.", new BigDecimal("2999"), "Accessories", "/images/products/Keyboard.jpg", 18, sellerId, "Corsair"),
                new Product("ANC Studio Headphones", "Over-ear wireless noise cancelling headphones with 40mm hi-res drivers.", new BigDecimal("4499"), "Accessories", "/images/products/headphones.jpg", 14, sellerId, "Sony"),
                new Product("Ultra Slim 4K Monitor", "27-inch IPS 4K UHD frameless monitor with HDR10 and 99% sRGB coverage.", new BigDecimal("15999"), "Electronics", "/images/products/smart-tv.jpg", 8, sellerId, "LG"),
                new Product("Ergonomic Memory Pillow", "Contour cervical memory foam pillow with cooling gel layer for neck support.", new BigDecimal("1299"), "Home & Kitchen", "/images/products/default.jpg", 25, sellerId, "Sleepyhead"),
                new Product("Deep Tissue Foam Roller", "High density grid foam roller for muscle recovery and physical therapy.", new BigDecimal("899"), "Sports & Fitness", "/images/products/yoga-mat.jpg", 22, sellerId, "Boldfit")
        );

        int newlyAdded = 0;
        for (Product p : defaults) {
            if (!productRepository.existsByName(p.getName())) {
                productRepository.save(p);
                newlyAdded++;
            }
        }
        if (newlyAdded > 0) {
            log.info("Seeded {} new products. Total catalog size: {}", newlyAdded, productRepository.count());
        }

        // Ensure all products have a seller ID
        productRepository.findAll().stream()
                .filter(product -> product.getSellerId() == null)
                .forEach(product -> {
                    product.setSellerId(sellerId);
                    productRepository.save(product);
                });
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

        Category electronics = categories.get("Electronics");
        Category mobiles = category(repository, "Mobiles", electronics);
        category(repository, "Smartphones", mobiles);
        category(repository, "Feature Phones", mobiles);
        category(repository, "Mobile Accessories", mobiles);
        Category computers = category(repository, "Computers", electronics);
        Category laptops = category(repository, "Laptops", computers);
        category(repository, "Desktops", computers);
        Category monitors = category(repository, "Monitors", computers);
        Category computerAccessories = category(repository, "Computer Accessories", computers);
        Category audio = category(repository, "Audio", electronics);
        category(repository, "Headphones", audio);
        category(repository, "Earbuds", audio);
        category(repository, "Speakers", audio);
        category(repository, "Microphones", audio);
        Category wearables = category(repository, "Wearables", electronics);

        Category fashion = categories.get("Fashion");
        Category mensFashion = category(repository, "Men's Fashion", fashion);
        category(repository, "Women's Fashion", fashion);

        Category homeKitchen = categories.get("Home & Kitchen");
        Category kitchenAppliances = category(repository, "Kitchen Appliances", homeKitchen);
        Category homeAppliances = category(repository, "Home Appliances", homeKitchen);
        Category furniture = category(repository, "Furniture", homeKitchen);

        Category sportsFitness = categories.get("Sports & Fitness");
        Category gymFitness = category(repository, "Gym & Fitness", sportsFitness);
        Category running = category(repository, "Running", sportsFitness);

        productRepository.findAll().forEach(product -> {
            if (product.getCategoryEntity() != null) return;
            Category target = switch (product.getName()) {
                case "Laptop", "Gaming Laptop" -> laptops;
                case "Smartphone" -> category(repository, "Smartphones", mobiles);
                case "Tablet" -> category(repository, "Desktops", computers);
                case "Smart TV", "Ultra Slim 4K Monitor" -> monitors;
                case "Headphones", "ANC Studio Headphones" -> category(repository, "Headphones", audio);
                case "Wireless Earbuds" -> category(repository, "Earbuds", audio);
                case "Bluetooth Speaker" -> category(repository, "Speakers", audio);
                case "USB Microphone" -> category(repository, "Microphones", audio);
                case "Bluetooth Neckband" -> audio;
                case "Keyboard", "RGB Gaming Keyboard", "Gaming Mouse", "Wireless Mouse", "USB Hub", "Laptop Stand", "Webcam" -> computerAccessories;
                case "Smart Watch", "Smart Watch Pro", "Fitness Band" -> wearables;
                case "Power Bank", "Charging Cable" -> category(repository, "Mobile Accessories", mobiles);
                case "Electric Kettle", "Mixer Grinder" -> kitchenAppliances;
                case "Desk Lamp", "Digital Scale" -> homeAppliances;
                case "Ergonomic Memory Pillow" -> furniture;
                case "Python Programming Book" -> category(repository, "Operating Systems", computerScience);
                case "Machine Learning Book" -> category(repository, "Machine Learning", computerScience);
                case "DSA Handbook" -> category(repository, "Data Structures & Algorithms", computerScience);
                case "DBMS Book" -> category(repository, "Database Management Systems", computerScience);
                case "Cotton T-Shirt" -> mensFashion;
                case "College Backpack", "Laptop Backpack" -> mensFashion;
                case "Running Shoes" -> running;
                case "Yoga Mat", "Dumbbell Set", "Deep Tissue Foam Roller" -> gymFitness;
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
            String currentImageUrl = product.getImageUrl();
            if (currentImageUrl == null || currentImageUrl.isBlank()) {
                product.setImageUrl("/images/products/default.jpg");
                productRepository.save(product);
            }
        });
    }
}
