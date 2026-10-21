// package com.example.backend.config;

// import com.example.helpdesk.model.Category;
// import com.example.helpdesk.model.Role;
// import com.example.helpdesk.model.User;
// import com.example.helpdesk.repository.CategoryRepository;
// import com.example.helpdesk.repository.UserRepository;
// import com.example.helpdesk.security.PasswordHasher;
// import java.util.List;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// /**
//  * Tạo dữ liệu ban đầu khi khởi động: danh mục mẫu, một Admin và một Agent.
//  * Không có API cấp quyền, nên tài khoản Admin/Agent chỉ được tạo từ biến môi trường.
//  */
// @Configuration
// public class SeedData {
//     private static final List<String> CATEGORIES = List.of("Phần cứng", "Mạng", "Phần mềm", "Tài khoản");

//     @Bean
//     CommandLineRunner initialData(CategoryRepository categories, UserRepository users, PasswordHasher passwords,
//                                   @Value("${ADMIN_EMAIL:}") String adminEmail,
//                                   @Value("${ADMIN_PASSWORD:}") String adminPassword,
//                                   @Value("${AGENT_EMAIL:}") String agentEmail,
//                                   @Value("${AGENT_PASSWORD:}") String agentPassword) {
//         return args -> {
//             for (String name : CATEGORIES) {
//                 if (!categories.existsByNameIgnoreCase(name)) {
//                     Category category = new Category();
//                     category.setName(name);
//                     categories.save(category);
//                 }
//             }
//             createIfMissing(users, passwords, adminEmail, adminPassword, "Demo Admin", Role.ADMIN);
//             createIfMissing(users, passwords, agentEmail, agentPassword, "Demo Agent", Role.AGENT);
//         };
//     }

//     private static void createIfMissing(UserRepository users, PasswordHasher passwords, String email,
//                                         String password, String fullName, Role role) {
//         if (email.isBlank() || password.isBlank() || users.existsByEmail(email.toLowerCase())) {
//             return;
//         }
//         User user = new User();
//         user.setEmail(email.toLowerCase());
//         user.setFullName(fullName);
//         user.setPasswordHash(passwords.hash(password));
//         user.setRole(role);
//         users.save(user);
//     }
// }
