"""Exact-evidence cloze questions: no model-created facts or answer keys."""
import random
import re
from .models import Question

STOP=set('About After Before Could Would Should Their There These Those Where Which While Today First Latest Being Under Through Other People Years News Says Said With From Have Into That This Will More Than They What Your Over Also'.casefold().split())

def build_quiz(stories):
    questions=[]
    pool={w for s in stories for w in re.findall(r'\b[A-Za-z]{5,}\b',s.title) if w.casefold() not in STOP}
    for story in stories:
        words=[w for w in re.findall(r'\b[A-Za-z]{5,}\b',story.title) if w.casefold() not in STOP]
        if not words: continue
        answer=max(words,key=len)
        alternatives=sorted(w for w in pool if w.casefold()!=answer.casefold() and
            w.casefold() not in story.title.casefold())
        if len(alternatives)<2: continue
        choices=[answer,*alternatives[:2]]
        random.Random(story.id).shuffle(choices)
        question='Complete the headline you read: “'+re.sub(r'\b'+re.escape(answer)+r'\b','____',story.title)+'”'
        questions.append(Question(id='q-'+story.id,question=question,choices=choices,
            correct_index=choices.index(answer),explanation='The story headline says: “'+story.title+'”.',
            story_id=story.id,fact_id=story.facts[0].id))
        if len(questions)==5: break
    return questions
