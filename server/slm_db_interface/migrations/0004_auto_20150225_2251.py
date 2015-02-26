# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
from django.conf import settings


class Migration(migrations.Migration):

    dependencies = [
        ('slm_db_interface', '0003_auto_20150225_1829'),
    ]

    operations = [
        migrations.AlterField(
            model_name='slmuser',
            name='borrower',
            field=models.OneToOneField(null=True, to='slm_db_interface.Borrower', related_name='slm_user', db_column='borrower_id'),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='slmuser',
            name='django_user',
            field=models.OneToOneField(to=settings.AUTH_USER_MODEL, related_name='slm_user', db_column='django_user_id'),
            preserve_default=True,
        ),
    ]
