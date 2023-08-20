# Copyright 2023 BentoML Team. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
from __future__ import annotations
import itertools
import typing as t

import pytest

import openllm

if t.TYPE_CHECKING:
  from openllm._types import LiteralRuntime

_WEIGHT_MAPPING = {"flan_t5": "google/flan-t5-small", "opt": "facebook/opt-125m", "baichuan": "baichuan-inc/Baichuan-7B", "chatglm": "THUDM/chatglm2-6b-int4",
                   "gpt_neox": "EleutherAI/pythia-410m-deduped", "falcon": "Rocketknight1/falcon-rw-1b", "llama": "NousResearch/Nous-Hermes-llama-2-7b",
                   "mpt": "mosaicml/mpt-7b-8k", "starcoder": "bigcode/gpt_bigcode-santacoder"}
_MODEL_TYPE_ALIAS_MAPPING = {"stablelm": "gpt_neox", "dolly-v2": "gpt_neox"}
_PROMPT_MAPPING = {"qa": "Answer the following yes/no question by reasoning step-by-step. Can you write a whole Haiku in a single tweet?",}

def parametrise_local_llm(model: str) -> t.Generator[tuple[str, openllm.LLMRunner[t.Any, t.Any] | openllm.LLM[t.Any, t.Any]], None, None]:
  if model not in _WEIGHT_MAPPING: pytest.skip(f"'{model}' is not yet supported in framework testing.")
  elif model in _MODEL_TYPE_ALIAS_MAPPING: model = _MODEL_TYPE_ALIAS_MAPPING[model]

  runtime_impl: tuple[LiteralRuntime, ...] = tuple()
  if model in openllm.MODEL_MAPPING_NAMES: runtime_impl += ("pt",)
  if model in openllm.MODEL_FLAX_MAPPING_NAMES: runtime_impl += ("flax",)
  if model in openllm.MODEL_TF_MAPPING_NAMES: runtime_impl += ("tf",)

  for framework, prompt in itertools.product(runtime_impl, _PROMPT_MAPPING.keys()): yield prompt, openllm.Runner(model, model_id=_WEIGHT_MAPPING[model], ensure_available=True, implementation=framework, device_map="auto", init_local=True)

def pytest_generate_tests(metafunc: pytest.Metafunc) -> None:
  if "prompt" in metafunc.fixturenames and "llm" in metafunc.fixturenames:
    metafunc.parametrize("prompt,llm", [(p, llm) for p, llm in parametrise_local_llm(metafunc.function.__name__[5:-15])])

def pytest_sessionfinish(session: pytest.Session, exitstatus: int):
  # If no tests are collected, pytest exists with code 5, which makes the CI fail.
  if exitstatus == 5: session.exitstatus = 0
