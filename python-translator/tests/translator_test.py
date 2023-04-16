import unittest
import os
from transformers.models.auto import AutoConfig, AutoTokenizer
from translator import Translator

dir_path = os.path.dirname(os.path.realpath(__file__))
translator = Translator(os.path.join(dir_path, '../../raw-files/t5.1.1.lm100k.large'))

class TranslatorTest(unittest.TestCase):

    def test_happy_path(self):
        expected_prefix = '<pad>concert_singer | select t1.concert_name from concert as t1'

        raw_input = ['Get concerts with the largest stadiums. | concert_singer | stadium : stadium_id, location, name, capacity, highest, lowest, average | singer : singer_id, name, country, song_name, song_release_year, age, is_male | concert : concert_id, concert_name, theme, stadium_id, year | singer_in_concert : concert_id, singer_id']
        # Example output!
        #['concert_singer | select t2.concert_name from stadium as t1 join concert as t2 on t1.stadium_id = t2.stadium_id order by t1.capacity desc limit 1']

        print(translator.translate(expected_prefix, raw_input))

if __name__ == '__main__':
    unittest.main()