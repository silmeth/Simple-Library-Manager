# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
from django.conf import settings


class Migration(migrations.Migration):

    replaces = [('slm_db_interface', '0001_initial'), ('slm_db_interface', '0002_auto_20141120_0018')]

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='Author',
            fields=[
                ('id', models.AutoField(serialize=False, db_column='author_id', primary_key=True)),
                ('name', models.CharField(db_index=True, max_length=100)),
            ],
            options={
                'db_table': 'authors',
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Book',
            fields=[
                ('id', models.AutoField(serialize=False, db_column='book_id', primary_key=True)),
                ('title', models.CharField(max_length=200)),
                ('isbn10', models.CharField(db_index=True, max_length=10, null=True)),
                ('isbn13', models.CharField(db_index=True, max_length=13, null=True)),
                ('published_year', models.SmallIntegerField(null=True)),
                ('borrow_date', models.DateField(null=True)),
                ('return_date', models.DateField(null=True)),
                ('author', models.ForeignKey(to='slm_db_interface.Author', db_column='author_id')),
            ],
            options={
                'db_table': 'books',
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Borrower',
            fields=[
                ('id', models.AutoField(serialize=False, db_column='borrower_id', primary_key=True)),
                ('nick', models.CharField(max_length=40)),
                ('name', models.CharField(max_length=40)),
                ('surname', models.CharField(max_length=40)),
                ('email', models.EmailField(max_length=75)),
            ],
            options={
                'db_table': 'borrowers',
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Publisher',
            fields=[
                ('id', models.AutoField(serialize=False, db_column='publisher_id', primary_key=True)),
                ('name', models.CharField(db_index=True, max_length=100)),
            ],
            options={
                'db_table': 'publishers',
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='SLMUser',
            fields=[
                ('id', models.AutoField(serialize=False, db_column='slm_user_id', primary_key=True)),
                ('can_borrow', models.BooleanField(default=True)),
                ('can_lend', models.BooleanField(default=False)),
                ('borrower', models.OneToOneField(db_column='borrower_id', to='slm_db_interface.Borrower')),
                ('django_user', models.OneToOneField(db_column='django_user_id', to=settings.AUTH_USER_MODEL)),
                ('can_manage_books', models.BooleanField(default=False)),
            ],
            options={
                'db_table': 'slm_users',
            },
            bases=(models.Model,),
        ),
        migrations.AddField(
            model_name='book',
            name='borrower',
            field=models.ForeignKey(to='slm_db_interface.Borrower', db_column='borrower_id'),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='book',
            name='publisher',
            field=models.ForeignKey(to='slm_db_interface.Publisher', db_column='publisher_id'),
            preserve_default=True,
        ),
    ]
