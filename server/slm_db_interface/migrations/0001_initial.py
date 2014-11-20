# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations
from django.conf import settings


class Migration(migrations.Migration):

    dependencies = [
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.CreateModel(
            name='Author',
            fields=[
                ('id', models.AutoField(db_column='author_id', primary_key=True, serialize=False)),
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
                ('id', models.AutoField(db_column='book_id', primary_key=True, serialize=False)),
                ('title', models.CharField(max_length=200)),
                ('isbn10', models.CharField(db_index=True, max_length=10, null=True)),
                ('isbn13', models.CharField(db_index=True, max_length=13, null=True)),
                ('published_year', models.SmallIntegerField(null=True)),
                ('borrow_date', models.DateField(null=True)),
                ('return_date', models.DateField(null=True)),
                ('author', models.ForeignKey(db_column='author_id', to='slm_db_interface.Author')),
            ],
            options={
                'db_table': 'books',
            },
            bases=(models.Model,),
        ),
        migrations.CreateModel(
            name='Borrower',
            fields=[
                ('id', models.AutoField(db_column='borrower_id', primary_key=True, serialize=False)),
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
                ('id', models.AutoField(db_column='publisher_id', primary_key=True, serialize=False)),
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
                ('id', models.AutoField(db_column='slm_user_id', primary_key=True, serialize=False)),
                ('can_borrow', models.BooleanField()),
                ('can_lend', models.BooleanField()),
                ('borrower', models.OneToOneField(db_column='borrower_id', to='slm_db_interface.Borrower')),
                ('django_user', models.OneToOneField(db_column='django_user_id', to=settings.AUTH_USER_MODEL)),
            ],
            options={
                'db_table': 'slm_users',
            },
            bases=(models.Model,),
        ),
        migrations.AddField(
            model_name='book',
            name='borrower',
            field=models.ForeignKey(db_column='borrower_id', to='slm_db_interface.Borrower'),
            preserve_default=True,
        ),
        migrations.AddField(
            model_name='book',
            name='publisher',
            field=models.ForeignKey(db_column='publisher_id', to='slm_db_interface.Publisher'),
            preserve_default=True,
        ),
    ]
