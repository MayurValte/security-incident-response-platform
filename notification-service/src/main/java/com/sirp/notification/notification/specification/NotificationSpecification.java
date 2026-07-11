package com.sirp.notification.notification.specification;

import com.sirp.notification.notification.entity.Notification;
import com.sirp.notification.notification.enums.NotificationChannel;
import com.sirp.notification.notification.enums.NotificationStatus;
import com.sirp.notification.notification.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationSpecification {

    private NotificationSpecification() {
    }

    public static Specification<Notification> incidentId(UUID incidentId) {

        return (root, query, cb) ->

            incidentId == null

                ? cb.conjunction()

                : cb.equal(root.get("incidentId"), incidentId);

    }

    public static Specification<Notification> recipientId(UUID recipientId) {

        return (root, query, cb) ->

            recipientId == null

                ? cb.conjunction()

                : cb.equal(root.get("recipientId"), recipientId);

    }

    public static Specification<Notification> status(NotificationStatus status) {

        return (root, query, cb) ->

            status == null

                ? cb.conjunction()

                : cb.equal(root.get("status"), status);

    }

    public static Specification<Notification> channel(NotificationChannel channel) {

        return (root, query, cb) ->

            channel == null

                ? cb.conjunction()

                : cb.equal(root.get("channel"), channel);

    }

    public static Specification<Notification> type(NotificationType type) {

        return (root, query, cb) ->

            type == null

                ? cb.conjunction()

                : cb.equal(root.get("type"), type);

    }

    public static Specification<Notification> createdAfter(Instant instant) {

        return (root, query, cb) ->

            instant == null

                ? cb.conjunction()

                : cb.greaterThanOrEqualTo(root.get("createdAt"), instant);

    }

    public static Specification<Notification> createdBefore(Instant instant) {

        return (root, query, cb) ->

            instant == null

                ? cb.conjunction()

                : cb.lessThanOrEqualTo(root.get("createdAt"), instant);

    }

}