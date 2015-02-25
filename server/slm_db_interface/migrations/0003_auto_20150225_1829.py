# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('slm_db_interface', '0002_auto_20141120_0103'),
    ]

    operations = [
        migrations.AlterField(
            model_name='borrower',
            name='nick',
            field=models.CharField(blank=True, max_length=40),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='borrower',
            name='surname',
            field=models.CharField(blank=True, max_length=40),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='slmuser',
            name='borrower',
            field=models.OneToOneField(to='slm_db_interface.Borrower', db_column='borrower_id', null=True),
            preserve_default=True,
        ),
    ]
