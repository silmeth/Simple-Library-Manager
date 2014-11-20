# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('slm_db_interface', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='slmuser',
            name='can_manage_books',
            field=models.BooleanField(default=False),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='slmuser',
            name='can_borrow',
            field=models.BooleanField(default=True),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='slmuser',
            name='can_lend',
            field=models.BooleanField(default=False),
            preserve_default=True,
        ),
    ]
