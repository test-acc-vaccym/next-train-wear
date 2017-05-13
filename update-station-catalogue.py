#!/usr/bin/env python3

import datetime
import itertools
import os
from pathlib import Path
from xml.etree import ElementTree

import click
import requests


@click.command()
@click.option('-n', '--user', required=True, help='NS API username')
@click.option('-p', '--password', required=True, prompt=True, hide_input=True, help='NS API password')
def main(user, password):
    """Update station catalogue from NS API."""

    response = requests.get('http://webservices.ns.nl/ns-api-stations-v2', auth=(user, password))
    response.raise_for_status()

    catalogue_path = Path(__file__).parent / 'app' / 'src' / 'main' / 'java' / 'me' / 'eigenein' / 'nexttrainwear' / 'StationCatalogue.java'
    with catalogue_path.open('wt', encoding='utf-8') as catalogue_file:
        print(os.linesep.join([
            'package me.eigenein.nexttrainwear;',
            '',
            '/**',
            ' * Station catalogue.',
            f' * Auto-generated on {datetime.datetime.now()}.',
            ' */',
            'public class StationCatalogue {',
            '',
            '    public static final Station[] STATIONS = {',
        ]), file=catalogue_file)

        stations_element = ElementTree.fromstring(response.content)
        for station_element in stations_element:
            code = station_element.find('Code').text
            land = station_element.find('Land').text
            long_name = station_element.find('Namen').find('Lang').text
            latitude = station_element.find('Lat').text
            longitude = station_element.find('Lon').text
            print(f'        new Station("{code}", "{long_name}", "{land}", {latitude}f, {longitude}f),', file=catalogue_file)

        print(os.linesep.join([
            '    };',
            '',
            '    private StationCatalogue() {',
            '        // Do nothing.',
            '    }',
            '}',
        ]), file=catalogue_file)


if __name__ == '__main__':
    main()