package com.example.backend.service;

import com.example.backend.model.Category;
import com.example.backend.model.Role;
import com.example.backend.model.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.TicketRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final TicketRepository tickets;

    public CategoryService(CategoryRepository categories, TicketRepository tickets) {
        this.categories = categories;
        this.tickets = tickets;
    }

    public record CategoryView(Long id, String name) {
        static CategoryView of(Category category) {
            return new CategoryView(category.getId(), category.getName());
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryView> list() {
        return categories.findAllByOrderByNameAsc().stream().map(CategoryView::of).toList();
    }

    @Transactional
    public CategoryView create(String name, User actor) {
        requireAdmin(actor);
        String trimmed = name.trim();
        if (categories.existsByNameIgnoreCase(trimmed)) {
            throw BusinessException.conflict("Category already exists");
        }
        Category category = new Category();
        category.setName(trimmed);
        return CategoryView.of(categories.save(category));
    }

    @Transactional
    public CategoryView rename(Long id, String name, User actor) {
        requireAdmin(actor);
        Category category = find(id);
        String trimmed = name.trim();
        if (!category.getName().equalsIgnoreCase(trimmed) && categories.existsByNameIgnoreCase(trimmed)) {
            throw BusinessException.conflict("Category already exists");
        }
        category.setName(trimmed);
        return CategoryView.of(category);
    }

    /** Chỉ xóa được danh mục chưa có ticket nào dùng. */
    @Transactional
    public void delete(Long id, User actor) {
        requireAdmin(actor);
        Category category = find(id);
        if (tickets.countByCategoryId(id) > 0) {
            throw BusinessException.conflict("Category is used by tickets");
        }
        categories.delete(category);
    }

    private Category find(Long id) {
        return categories.findById(id).orElseThrow(() -> BusinessException.notFound("Category not found"));
    }

    private static void requireAdmin(User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw BusinessException.forbidden("Admin only");
        }
    }
}
