package ru.bre.storage.repository;

import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import ru.bre.storage.entity.Feedback;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class FeedbackRepository {

    private final SessionFactory sessionFactory;

    @Autowired
    public FeedbackRepository(@Lazy SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Transactional
    public List<Feedback> getFeedbackFromDate(LocalDateTime fromDate) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "FROM Feedback f WHERE f.createdAt >= :fromDate ORDER BY f.createdAt DESC";
            Query<Feedback> query = session.createQuery(hql, Feedback.class);
            query.setParameter("fromDate", fromDate);
            return query.list();
        }
    }
}
