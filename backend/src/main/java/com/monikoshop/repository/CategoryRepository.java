package com.monikoshop.repository;

import com.monikoshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    /** دسته‌های اصلی (بدون والد) به ترتیب sortOrder */
    List<Category> findByParentIsNullOrderBySortOrderAscIdAsc();

    /**
     * زیر‌دسته‌های یک والد.
     * فیلد entity = parent (نه parentId)، پس باید از parent_Id استفاده شود.
     */
    List<Category> findByParent_IdOrderBySortOrderAscIdAsc(Long parentId);

    /** درخت کامل برای مگامنو: دسته‌های ریشه با children لود شده */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL ORDER BY c.sortOrder ASC, c.id ASC")
    List<Category> findRootCategoriesWithChildren();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
