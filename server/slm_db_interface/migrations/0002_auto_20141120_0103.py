# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('slm_db_interface', '0001_squashed_0002_auto_20141120_0018'),
    ]

    operations = [
        migrations.AlterField(
            model_name='book',
            name='borrow_date',
            field=models.DateField(null=True, db_index=True, blank=True),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='book',
            name='borrower',
            field=models.ForeignKey(null=True, to='slm_db_interface.Borrower', blank=True, db_column='borrower_id'),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='book',
            name='isbn10',
            field=models.CharField(null=True, blank=True, db_index=True, max_length=10),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='book',
            name='isbn13',
            field=models.CharField(null=True, blank=True, db_index=True, max_length=13),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='book',
            name='published_year',
            field=models.SmallIntegerField(null=True, db_index=True, blank=True),
            preserve_default=True,
        ),
        migrations.AlterField(
            model_name='book',
            name='return_date',
            field=models.DateField(null=True, db_index=True, blank=True),
            preserve_default=True,
        ),
    ]
