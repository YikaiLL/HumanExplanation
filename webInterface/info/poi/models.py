# This is an auto-generated Django model module.
# You'll have to do the following manually to clean this up:
#   * Rearrange models' order
#   * Make sure each model has one field with primary_key=True
#   * Make sure each ForeignKey has `on_delete` set to the desired behavior.
#   * Remove `managed = False` lines if you wish to allow Django to create, modify, and delete the table
# Feel free to rename the models, but don't rename db_table values or field names.
from __future__ import unicode_literals

from django.db import models


class AuthGroup(models.Model):
    name = models.CharField(unique=True, max_length=80)

    class Meta:
        managed = False
        db_table = 'auth_group'


class AuthGroupPermissions(models.Model):
    group = models.ForeignKey(AuthGroup, models.DO_NOTHING)
    permission = models.ForeignKey('AuthPermission', models.DO_NOTHING)

    class Meta:
        managed = False
        db_table = 'auth_group_permissions'
        unique_together = (('group', 'permission'),)


class AuthPermission(models.Model):
    name = models.CharField(max_length=255)
    content_type = models.ForeignKey('DjangoContentType', models.DO_NOTHING)
    codename = models.CharField(max_length=100)

    class Meta:
        managed = False
        db_table = 'auth_permission'
        unique_together = (('content_type', 'codename'),)


class AuthUser(models.Model):
    password = models.CharField(max_length=128)
    last_login = models.DateTimeField(blank=True, null=True)
    is_superuser = models.IntegerField()
    username = models.CharField(unique=True, max_length=150)
    first_name = models.CharField(max_length=30)
    last_name = models.CharField(max_length=30)
    email = models.CharField(max_length=254)
    is_staff = models.IntegerField()
    is_active = models.IntegerField()
    date_joined = models.DateTimeField()

    class Meta:
        managed = False
        db_table = 'auth_user'


class AuthUserGroups(models.Model):
    user = models.ForeignKey(AuthUser, models.DO_NOTHING)
    group = models.ForeignKey(AuthGroup, models.DO_NOTHING)

    class Meta:
        managed = False
        db_table = 'auth_user_groups'
        unique_together = (('user', 'group'),)


class AuthUserUserPermissions(models.Model):
    user = models.ForeignKey(AuthUser, models.DO_NOTHING)
    permission = models.ForeignKey(AuthPermission, models.DO_NOTHING)

    class Meta:
        managed = False
        db_table = 'auth_user_user_permissions'
        unique_together = (('user', 'permission'),)


class BestExp(models.Model):
    id = models.IntegerField(primary_key=True)
    user_id = models.CharField(max_length=45, blank=True, null=True)
    venue_id = models.CharField(max_length=45, blank=True, null=True)
    best_exp = models.CharField(max_length=45, blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'best_exp'


class CandExp(models.Model):
    user_id = models.IntegerField(blank=True, null=True)
    venue_id = models.CharField(max_length=45, blank=True, null=True)
    cand_exp = models.CharField(max_length=45, blank=True, null=True)
    id = models.CharField(primary_key=True, max_length=45)

    class Meta:
        managed = False
        db_table = 'cand_exp'


class DjangoAdminLog(models.Model):
    action_time = models.DateTimeField()
    object_id = models.TextField(blank=True, null=True)
    object_repr = models.CharField(max_length=200)
    action_flag = models.SmallIntegerField()
    change_message = models.TextField()
    content_type = models.ForeignKey('DjangoContentType', models.DO_NOTHING, blank=True, null=True)
    user = models.ForeignKey(AuthUser, models.DO_NOTHING)

    class Meta:
        managed = False
        db_table = 'django_admin_log'


class DjangoContentType(models.Model):
    app_label = models.CharField(max_length=100)
    model = models.CharField(max_length=100)

    class Meta:
        managed = False
        db_table = 'django_content_type'
        unique_together = (('app_label', 'model'),)


class DjangoMigrations(models.Model):
    app = models.CharField(max_length=255)
    name = models.CharField(max_length=255)
    applied = models.DateTimeField()

    class Meta:
        managed = False
        db_table = 'django_migrations'


class DjangoSession(models.Model):
    session_key = models.CharField(primary_key=True, max_length=40)
    session_data = models.TextField()
    expire_date = models.DateTimeField()

    class Meta:
        managed = False
        db_table = 'django_session'


class PoiList(models.Model):
    userid = models.CharField(max_length=255, blank=True, null=True)
    venue1 = models.CharField(max_length=255, blank=True, null=True)
    venue2 = models.CharField(max_length=255, blank=True, null=True)
    venue3 = models.CharField(max_length=255, blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'poi_list'


class TwitterUserTopic(models.Model):
    userid = models.CharField(db_index=True, max_length=255, blank=True, null=True)
    topic = models.CharField(max_length=255, blank=True, null=True)
    frequency = models.IntegerField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'twitter_user_topic'


class User(models.Model):
    userid = models.CharField(primary_key=True, max_length=255)
    username = models.TextField(blank=True, null=True)
    description = models.TextField(blank=True, null=True)
    usercity = models.TextField(blank=True, null=True)
    usercountry = models.TextField(blank=True, null=True)
    profile_image = models.TextField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'user'


class UserHistory(models.Model):
    id = models.CharField(primary_key=True, max_length=255)
    userid = models.CharField(max_length=255, blank=True, null=True)
    venueid = models.TextField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'user_history'


class Venue(models.Model):
    venueid = models.CharField(primary_key=True, max_length=255)
    venuename = models.TextField(blank=True, null=True)
    venueaddress = models.TextField(blank=True, null=True)
    venuecity = models.TextField(blank=True, null=True)
    venuerating = models.TextField(blank=True, null=True)
    venuephoto = models.TextField(blank=True, null=True)
    venuedescription = models.TextField(blank=True, null=True)
    venuepricetier = models.TextField(blank=True, null=True)
    venuepricemessage = models.TextField(blank=True, null=True)
    venuelikes = models.FloatField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'venue'


class VenueCategories(models.Model):
    venueid = models.CharField(max_length=255, blank=True, null=True)
    venuecategories = models.TextField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'venue_categories'


class VenueComments(models.Model):
    venueid = models.CharField(max_length=255, blank=True, null=True)
    venuecomments = models.TextField(blank=True, null=True)
    venuephrases = models.TextField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'venue_comments'


class VenueTags(models.Model):
    venueid = models.CharField(max_length=255, blank=True, null=True)
    venuetags = models.TextField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = 'venue_tags'
